package `fun`.fan.xc.plugin.proxy.interceptor

import `fun`.fan.xc.plugin.proxy.config.ProxyConfigurationManager
import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockDataLoader
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockMatcher
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockResponseHandler
import `fun`.fan.xc.plugin.proxy.orchestrator.ProxyOrchestrator
import `fun`.fan.xc.plugin.proxy.transformer.HttpResponseTransformer
import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.event.EventImpl
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 代理拦截器
 * 专注于拦截Spring MVC请求，判断是否需要代理，并委托ProxyOrchestrator处理具体的代理逻辑
 *
 * @author fan
 *
 * ## 职责范围
 * - 拦截Spring MVC请求
 * - 判断是否需要代理
 * - 基础的请求合法性校验
 * - 转发代理请求给ProxyOrchestrator
 * - 统一的错误响应处理
 *
 * ## 不负责的功能
 * - 请求构建（已移至HttpRequestTransformer）
 * - 响应处理（已移至HttpResponseTransformer）
 * - 连接池管理（已移至HostPortChannelPool）
 * - 负载均衡（已移至ProxyLoadBalancer）
 */
class ProxyInterceptor(
    private val properties: ProxyProperties,
    private val orchestrator: ProxyOrchestrator,
    private val configManager: ProxyConfigurationManager,
    private val responseTransformer: HttpResponseTransformer,
    private val mockDataLoader: ProxyMockDataLoader,
    private val mockMatcher: ProxyMockMatcher,
    private val mockResponseHandler: ProxyMockResponseHandler
) : HandlerInterceptor {

    private val log: Logger = LoggerFactory.getLogger(ProxyInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            // 步骤1：拦截器拦截请求
            return executeProxyFlow(request, response)
        } catch (e: ProxyException) {
            // 检查是否为Mock已处理的特殊异常
            if (e.message == "MOCK_HANDLED") {
                return false // Mock已处理完成，不再进入Controller
            }
            throw e
        } catch (e: Exception) {
            log.error("Unexpected error in proxy interceptor", e)
            throw ProxyException(ReturnCode.SYSTEM_ERROR, e.message)
        }
    }

    /**
     * 执行代理流程（委托给ProxyOrchestrator），支持Mock功能
     */
    private fun executeProxyFlow(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val startTime = System.currentTimeMillis()

        val result = runBlocking {
            // 步骤2：统一入口判断是否代理
            val requestPath = request.requestURI
            val matchedConfig = configManager.findProxyConfig(requestPath)
                ?: throw ProxyException("No proxy configuration found for path: $requestPath")

            // 检查是否启用Mock功能
            if (isMockEnabled(matchedConfig)) {
                log.debug("检测到Mock功能启用，尝试处理Mock响应: {}", requestPath)
                handleMockRequest(request, response, matchedConfig)
                // Mock响应已直接处理，这里返回一个特殊结果避免后续处理
                throw ProxyException("MOCK_HANDLED") // 特殊异常，用于标识Mock已处理
            }

            // 使用路由特定超时，如果未设置则使用全局超时
            val effectiveTimeout = if (matchedConfig.timeout > 0) matchedConfig.timeout else properties.timeout

            // 使用路由特定重试次数，如果未设置则使用全局重试次数
            val effectiveRetryCount =
                if (matchedConfig.retryCount > 0) matchedConfig.retryCount else properties.retryCount

            // 执行完整的代理流程（步骤3-7）
            orchestrator.executeProxyFlow(request, matchedConfig, effectiveTimeout, effectiveRetryCount)
        }

        // 使用HttpResponseTransformer处理响应
        responseTransformer.transform(response, result)

        log.info(
            "Proxy request completed: {} -> status: {}, executionTime: {}ms, target: {}",
            request.requestURI, result.statusCode, System.currentTimeMillis() - startTime, result.target
        )

        return false // 已处理完成，不再进入Controller
    }

    /**
     * 检查是否启用Mock功能
     */
    private fun isMockEnabled(matchedConfig: ProxyConfigurationManager.ProxyConfigMatch): Boolean {
        return matchedConfig.route.mock != null &&
                matchedConfig.route.mock.enabled == true &&
                matchedConfig.route.mock.dataSource != null
    }

    /**
     * 处理Mock请求
     */
    private fun handleMockRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        matchedConfig: ProxyConfigurationManager.ProxyConfigMatch
    ) {
        try {
            // 加载Mock数据
            val mockData = mockDataLoader.loadMockData(matchedConfig.route.mock!!.dataSource!!)

            // 获取请求参数
            val requestParams = EventImpl.getEvent().getParamMap()

            // 查找最佳匹配的Mock配置
            val mockConfig = mockMatcher.findBestMatch(
                request.requestURI,
                request.method,
                requestParams,
                mockData
            )

            if (mockConfig == null) {
                log.warn("未找到匹配的Mock配置: {} {}", request.method, request.requestURI)
                throw ProxyException("No matching mock configuration found for: ${request.method} ${request.requestURI}")
            }

            // 验证Mock配置
            if (!mockResponseHandler.validateMockConfig(mockConfig)) {
                log.error("Mock配置验证失败")
                throw ProxyException("Invalid mock configuration")
            }

            // 处理Mock响应
            mockResponseHandler.handleMockResponse(mockConfig, request, response)

            log.info(
                "Mock response handled: {} {} -> status: {}",
                request.method, request.requestURI, response.status
            )

        } catch (e: Exception) {
            log.error("处理Mock请求失败: {} {}", request.method, request.requestURI, e)
            throw ProxyException("Failed to handle mock request", e)
        }
    }
}
