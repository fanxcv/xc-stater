package `fun`.fan.xc.plugin.proxy.orchestrator

import `fun`.fan.xc.plugin.proxy.client.HostPortChannelPool
import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.config.ProxyConfigurationManager
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.handler.ProxyLoadBalancer
import `fun`.fan.xc.plugin.proxy.transformer.HttpRequestTransformer
import io.netty.channel.Channel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeoutException
import jakarta.servlet.http.HttpServletRequest

/**
 * 代理流程统一协调器
 * 负责协调整个代理请求的7步流程：
 * 1. 拦截器拦截请求 → 2. 统一入口判断是否代理 → 3. 从负载均衡器获取目标地址 →
 * 4. 从连接池获取连接 → 5. 构建新请求(透传参数/消息体/头部) → 6. 执行请求 →
 * 7. 获取结果返回 → 8. 释放连接回连接池
 *
 * @author fan
 *
 * ## 功能特性
 * - 统一的代理流程协调入口
 * - 完整的异常处理和错误恢复
 * - 请求链路追踪和监控统计
 * - 组件生命周期管理
 * - 支持同步和异步请求处理
 * - 统一的超时控制和重试机制
 *
 * ## 使用示例
 * ```kotlin
 * val orchestrator = ProxyOrchestrator(proxyRequestHandler, proxyClient)
 * val response = orchestrator.executeProxyFlow(request, response, requestPath)
 * ```
 */
class ProxyOrchestrator(
    private val client: ProxyClient,
    private val connectionPool: HostPortChannelPool,
    private val configManager: ProxyConfigurationManager,
    private val requestTransformer: HttpRequestTransformer = HttpRequestTransformer()
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyOrchestrator::class.java)

    /**
     * 执行完整的代理流程
     *
     * @param request 原始HTTP请求
     * @param requestPath 请求路径
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理执行结果
     * @throws ProxyException 代理执行异常
     */
    suspend fun executeProxyFlow(
        request: HttpServletRequest,
        requestPath: String,
        timeoutMs: Long
    ): ProxyClient.ProxyResponse {
        try {
            return withTimeout(timeoutMs) {
                executeInternalProxyFlow(request, requestPath, timeoutMs - 100)
            }
        } catch (e: Exception) {
            when (e) {
                is TimeoutCancellationException, is TimeoutException -> {
                    log.error("Proxy flow timeout for path: {}", requestPath)
                    throw ProxyException("Proxy request timeout: ${e.message}")
                }

                is ProxyException -> throw e
                else -> {
                    log.error("Unexpected error in proxy flow for path: {}", requestPath, e)
                    throw ProxyException("Unexpected proxy error: ${e.message}")
                }
            }
        }
    }

    /**
     * 执行内部代理流程（不包含超时处理）
     */
    private suspend fun executeInternalProxyFlow(
        request: HttpServletRequest,
        requestPath: String,
        timeoutMs: Long
    ): ProxyClient.ProxyResponse {
        // 步骤2：统一入口判断是否代理
        val matchedConfig = configManager.findProxyConfig(requestPath)
            ?: throw ProxyException("No proxy configuration found for path: $requestPath")

        // log.info(
        //     "Proxying request: {} to targets: {}", requestPath,
        //     matchedConfig.getTargetUris()
        // )

        // 获取重试次数配置
        val maxRetries = 3 // 默认重试3次

        // 执行带重试机制的代理流程
        return executeWithRetry(request, matchedConfig, timeoutMs, maxRetries)
    }

    /**
     * 带重试机制的代理流程执行
     */
    private suspend fun executeWithRetry(
        request: HttpServletRequest,
        matchedConfig: ProxyConfigurationManager.ProxyConfigMatch,
        timeoutMs: Long,
        maxRetries: Int
    ): ProxyClient.ProxyResponse {
        var lastException: Exception? = null

        // 构建目标URL列表
        val weightedUrls = matchedConfig.targets.map { target ->
            ProxyLoadBalancer.WeightedUrl(target.uri, target.weight)
        }

        // 获取负载均衡器
        val loadBalancerKey = matchedConfig.route.source
        val loadBalancer = ProxyLoadBalancer.getOrCreateLoadBalancer(loadBalancerKey)

        for (attempt in 0..maxRetries) {
            // 步骤3：从负载均衡器获取本次的目标地址
            val selectedTarget = loadBalancer.selectTarget(weightedUrls)
                ?: throw ProxyException("No available target URL for route: $loadBalancerKey")

            try {
                // 提前转换URI对象，避免重复转换
                val targetUri = URI(selectedTarget.url)

                // 步骤4：根据目的地址从连接池获取一个连接
                val connection = connectionPool.acquireConnectionSuspend(targetUri)

                try {
                    // 步骤5：通过原始请求构建新的请求，包括url上的参数和消息体的透传, header的透传等
                    val proxiedRequest = requestTransformer.transform(request, targetUri, matchedConfig.pathWithinPattern)

                    // 步骤6：执行构建的请求，使用已获取的连接和URI对象
                    val result = client.executeProxyRequest(proxiedRequest, targetUri.toString(), connection, timeoutMs)

                    // 检查响应状态码
                    if (result.statusCode !in 200..299) {
                        markFailed(loadBalancer, selectedTarget.url)
                        // 继续重试
                        continue
                    }

                    return result
                } finally {
                    // 步骤7：释放连接回连接池
                    releaseConnectionToPool(connection)
                }
            } catch (e: Exception) {
                lastException = e
                log.warn("Proxy attempt ${attempt + 1} failed: ${e.message}")

                markFailed(loadBalancer, selectedTarget.url)

                // 如果是最后一次尝试，抛出异常
                if (attempt == maxRetries) {
                    throw ProxyException("Proxy failed after $maxRetries attempts: ${e.message}", e)
                }

                // 等待一段时间再重试
                delay(50 * (attempt + 1).toLong())
            }
        }

        // 这行代码理论上不会执行到，但为了编译通过还是加上
        throw lastException ?: ProxyException("Proxy failed after $maxRetries attempts")
    }

    private fun markFailed(loadBalancer: ProxyLoadBalancer, uri: String) {

        // 标记该节点为故障节点
        loadBalancer.markFailed(uri)
        // 检查是否所有节点都故障了，如果是则清除故障状态
        if (loadBalancer.areAllUrlsFailed()) {
            loadBalancer.clearAllFailures()
        }

    }

    /**
     * 步骤7：释放连接回连接池
     */
    private fun releaseConnectionToPool(connection: Channel) {
        try {
            connectionPool.releaseConnection(connection)
        } catch (e: Exception) {
            log.warn("Failed to release connection to pool: {}", e.message)
            // 释放连接失败不应该中断整个流程
        }
    }
}
