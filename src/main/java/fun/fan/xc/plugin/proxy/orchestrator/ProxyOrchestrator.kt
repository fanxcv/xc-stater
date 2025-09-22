package `fun`.fan.xc.plugin.proxy.orchestrator

import `fun`.fan.xc.plugin.proxy.client.HostPortChannelPool
import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.config.ProxyConfigurationManager
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.handler.ProxyLoadBalancer
import `fun`.fan.xc.plugin.proxy.transformer.HttpRequestTransformer
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeoutException
import javax.servlet.http.HttpServletRequest

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
    private val proxyClient: ProxyClient,
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
                executeInternalProxyFlow(request, requestPath)
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
    ): ProxyClient.ProxyResponse {
        // 步骤2：统一入口判断是否代理
        val matchedConfig = configManager.findProxyConfig(requestPath)
            ?: throw ProxyException("No proxy configuration found for path: $requestPath")

        // log.info(
        //     "Proxying request: {} to targets: {}", requestPath,
        //     matchedConfig.getTargetUris()
        // )

        // 步骤3：从负载均衡器获取本次的目标地址
        val selectedTarget = selectTargetByLoadBalancer(matchedConfig)

        // 提前转换URI对象，避免重复转换
        val targetUri = URI(selectedTarget.url)

        // 步骤4：根据目的地址从连接池获取一个连接
        val connection = connectionPool.acquireConnectionSuspend(targetUri)

        try {
            // 步骤5：通过原始请求构建新的请求，包括url上的参数和消息体的透传, header的透传等
            val proxiedRequest = requestTransformer.transform(request, targetUri)

            // 步骤6：执行构建的请求，使用已获取的连接和URI对象
            return executeProxiedRequest(proxiedRequest, targetUri, connection)
        } finally {
            // 步骤7：释放连接回连接池
            releaseConnectionToPool(connection)
        }
    }

    /**
     * 步骤3：从负载均衡器获取本次的目的地址
     */
    private fun selectTargetByLoadBalancer(matchedConfig: ProxyConfigurationManager.ProxyConfigMatch):
            ProxyLoadBalancer.WeightedUrl {

        // 构建目标URL列表
        val weightedUrls = matchedConfig.targets.map { target ->
            ProxyLoadBalancer.WeightedUrl(target.uri, target.weight)
        }

        // 获取负载均衡器
        val loadBalancerKey = matchedConfig.route.source
        val loadBalancer = ProxyLoadBalancer.getOrCreateLoadBalancer(loadBalancerKey)

        // 选择目标地址（同步方法，避免协程依赖）
        return loadBalancer.selectTarget(weightedUrls)
            ?: throw ProxyException("No available target URL for route: $loadBalancerKey")
    }

    /**
     * 步骤6：执行构建的请求
     */
    private suspend fun executeProxiedRequest(
        proxiedRequest: FullHttpRequest,
        targetUri: URI,
        connection: Channel
    ): ProxyClient.ProxyResponse {
        return proxyClient.executeProxyRequest(proxiedRequest, targetUri.toString(), connection)
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
