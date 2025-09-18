package `fun`.fan.xc.plugin.proxy.interceptor

import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.plugin.proxy.handler.ProxyRequestHandler
import io.netty.handler.codec.http.FullHttpRequest
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.TimeoutException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 代理拦截器
 * 拦截配置的代理请求，直接转发到目标服务器
 *
 * @author fan
 */
class ProxyInterceptor(private val proxyRequestHandler: ProxyRequestHandler) : HandlerInterceptor {
    private val log: Logger = LoggerFactory.getLogger(ProxyInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            // 检查是否需要代理此请求
            val proxyConfig =
                proxyRequestHandler.findMatchingProxyConfig(request.requestURI) ?: return true // 不需要代理，继续正常处理

            log.info(
                "Proxying request: {} to targets: {}", request.requestURI,
                proxyConfig.targets.map { it.uri })

            // 执行代理转发
            return handleProxyRequest(request, response, proxyConfig)

        } catch (e: ProxyException) {
            log.error("Proxy request failed: {}", e.message)
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            response.writer.write("Proxy request failed: ${e.message}")
            return false
        } catch (e: Exception) {
            log.error("Unexpected error in proxy interceptor", e)
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            response.writer.write("Internal server error")
            return false
        }
    }

    /**
     * 处理代理请求
     */
    private fun handleProxyRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        proxyConfig: ProxyRequestHandler.ProxyConfigMatch
    ): Boolean {
        try {
            // 构建Netty HTTP请求
            val nettyRequest = buildNettyHttpRequest(request)

            // 执行代理请求
            val proxyResponse = runBlocking {
                withTimeout(30000) {
                    proxyRequestHandler.handleProxyRequest(request.requestURI, nettyRequest)
                }
            }

            // 设置响应状态码
            response.status = proxyResponse.statusCode

            // 透传响应头
            proxyResponse.headers.entries.forEach { (name, value) ->
                // 跳过一些特殊的响应头
                when (name.lowercase()) {
                    "connection", "content-length", "transfer-encoding", "date", "server" -> {
                        // 跳过这些由Servlet容器自动设置的头
                    }

                    else -> {
                        response.setHeader(name, value)
                    }
                }
            }

            // 写入响应体
            if (proxyResponse.body.isNotEmpty()) {
                response.outputStream.write(proxyResponse.body)
                response.outputStream.flush()
            }

            log.info(
                "Proxy request completed: {} -> {}, status: {}, bodySize: {}",
                request.requestURI, proxyConfig.targets.first().uri, proxyResponse.statusCode, proxyResponse.body.size
            )

            return false // 已处理完成，不再进入Controller
        } catch (e: TimeoutException) {
            log.error("Proxy request timeout: {} : {}", request.requestURI, e.message)
            response.status = HttpServletResponse.SC_GATEWAY_TIMEOUT
            response.writer.write("Proxy request timeout: ${e.message}")
            return false
        } catch (e: Exception) {
            log.error("Failed to proxy request: {} : {}", request.requestURI, e.message)
            response.status = HttpServletResponse.SC_BAD_GATEWAY
            response.writer.write("Failed to proxy request: ${e.message}")
            return false
        }
    }

    /**
     * 构建Netty HTTP请求
     */
    private fun buildNettyHttpRequest(request: HttpServletRequest): FullHttpRequest {
        // 构建完整的URL（包含查询参数）
        val requestUrl = request.requestURI + (if (request.queryString != null) "?${request.queryString}" else "")

        // 读取请求体
        val requestBody = try {
            request.inputStream.readBytes()
        } catch (_: Exception) {
            ByteArray(0)
        }

        // 获取HTTP方法
        val method = getNettyHttpMethod(request.method)

        // 创建Netty请求
        val nettyRequest = io.netty.handler.codec.http.DefaultFullHttpRequest(
            io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
            method,
            requestUrl,
            io.netty.buffer.Unpooled.wrappedBuffer(requestBody)
        )

        // 复制Headers
        val headerNames = request.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            val headerValues = request.getHeaders(headerName)
            while (headerValues.hasMoreElements()) {
                val headerValue = headerValues.nextElement()
                // 跳过一些特殊headers，让Netty自动处理
                when (headerName.lowercase()) {
                    "content-length", "host", "connection", "keep-alive" -> {
                        // 跳过这些headers
                    }

                    else -> {
                        nettyRequest.headers().add(headerName, headerValue)
                    }
                }
            }
        }

        // 设置必要的headers
        if (!nettyRequest.headers().contains("Host")) {
            nettyRequest.headers().set("Host", request.serverName + ":" + request.serverPort)
        }

        if (!nettyRequest.headers().contains("Content-Length")) {
            nettyRequest.headers().set("Content-Length", requestBody.size.toString())
        }

        // 设置User-Agent
        if (!nettyRequest.headers().contains("User-Agent")) {
            nettyRequest.headers().set("User-Agent", "Xc-Proxy/1.0")
        }

        return nettyRequest
    }

    /**
     * 获取Netty HTTP方法
     */
    private fun getNettyHttpMethod(httpMethod: String): io.netty.handler.codec.http.HttpMethod {
        return when (httpMethod.uppercase()) {
            "GET" -> io.netty.handler.codec.http.HttpMethod.GET
            "POST" -> io.netty.handler.codec.http.HttpMethod.POST
            "PUT" -> io.netty.handler.codec.http.HttpMethod.PUT
            "DELETE" -> io.netty.handler.codec.http.HttpMethod.DELETE
            "PATCH" -> io.netty.handler.codec.http.HttpMethod.PATCH
            "OPTIONS" -> io.netty.handler.codec.http.HttpMethod.OPTIONS
            "HEAD" -> io.netty.handler.codec.http.HttpMethod.HEAD
            else -> io.netty.handler.codec.http.HttpMethod.valueOf(httpMethod)
        }
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // 请求完成后的清理工作
    }
}
