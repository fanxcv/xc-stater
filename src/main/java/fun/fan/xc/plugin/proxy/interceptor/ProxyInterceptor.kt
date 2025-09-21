package `fun`.fan.xc.plugin.proxy.interceptor

import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.orchestrator.ProxyOrchestrator
import `fun`.fan.xc.plugin.proxy.transformer.HttpResponseTransformer
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
    private val orchestrator: ProxyOrchestrator,
    private val responseTransformer: HttpResponseTransformer
) : HandlerInterceptor {

    private val log: Logger = LoggerFactory.getLogger(ProxyInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            // 步骤1：拦截器拦截请求
            return executeProxyFlow(request, response)
        } catch (e: ProxyException) {
            log.error("Proxy request failed: {}", e.message)
            handleErrorResponse(
                response, HttpServletResponse.SC_BAD_GATEWAY,
                "Proxy request failed: ${e.message}"
            )
            return false
        } catch (e: Exception) {
            log.error("Unexpected error in proxy interceptor", e)
            handleErrorResponse(
                response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal server error"
            )
            return false
        }
    }

    /**
     * 执行代理流程（委托给ProxyOrchestrator）
     */
    private fun executeProxyFlow(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val startTime = System.currentTimeMillis()

        // 执行完整的代理流程（步骤3-7）
        val result = runBlocking {
            orchestrator.executeProxyFlow(request, request.requestURI)
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
     * 统一的错误响应处理
     */
    private fun handleErrorResponse(response: HttpServletResponse, statusCode: Int, message: String) {
        try {
            response.status = statusCode
            response.characterEncoding = "UTF-8"
            response.contentType = "text/plain;charset=UTF-8"
            response.writer.write(message)
            response.writer.flush()

            log.debug("Error response sent: status={}, message={}", statusCode, message)

        } catch (e: Exception) {
            log.error("Failed to send error response: status={}, message={}", statusCode, message, e)
        }
    }
}
