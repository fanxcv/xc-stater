package `fun`.fan.xc.plugin.proxy.interceptor

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.orchestrator.ProxyOrchestrator
import `fun`.fan.xc.plugin.proxy.transformer.HttpResponseTransformer
import `fun`.fan.xc.starter.enums.ReturnCode
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
    private val responseTransformer: HttpResponseTransformer
) : HandlerInterceptor {

    private val log: Logger = LoggerFactory.getLogger(ProxyInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            // 步骤1：拦截器拦截请求
            return executeProxyFlow(request, response)
        } catch (e: ProxyException) {
            throw e
        } catch (e: Exception) {
            log.error("Unexpected error in proxy interceptor", e)
            throw ProxyException(ReturnCode.SYSTEM_ERROR, e.message)
        }
    }

    /**
     * 执行代理流程（委托给ProxyOrchestrator）
     */
    private fun executeProxyFlow(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val startTime = System.currentTimeMillis()

        // 执行完整的代理流程（步骤3-7）
        val result = runBlocking {
            orchestrator.executeProxyFlow(request, request.requestURI, properties.timeout)
        }

        // 使用HttpResponseTransformer处理响应
        responseTransformer.transform(response, result)

        log.info(
            "Proxy request completed: {} -> status: {}, executionTime: {}ms, target: {}",
            request.requestURI, result.statusCode, System.currentTimeMillis() - startTime, result.target
        )

        return false // 已处理完成，不再进入Controller
    }
}
