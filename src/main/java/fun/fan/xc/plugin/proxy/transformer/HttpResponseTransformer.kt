package `fun`.fan.xc.plugin.proxy.transformer

import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.servlet.http.HttpServletResponse

/**
 * HTTP响应转换器
 * 负责将Netty HTTP响应转换为Servlet响应，包括Header透传、状态码处理、响应体写入等
 *
 * @author fan
 *
 * ## 功能特性
 * - Netty响应到Servlet响应的完整转换
 * - Header的智能透传和过滤
 * - 状态码的正确处理
 * - 响应体的安全写入
 * - 支持各种响应类型
 * - 响应规范化处理
 * - 安全性响应头处理
 *
 * ## 使用示例
 * ```kotlin
 * val transformer = HttpResponseTransformer()
 * transformer.transform(servletResponse, proxyResponse)
 * ```
 */
class HttpResponseTransformer {

    private val log: Logger = LoggerFactory.getLogger(HttpResponseTransformer::class.java)

    /**
     * 需要过滤的响应Headers列表
     * 这些Header会由Servlet容器自动处理
     */
    private val filteredResponseHeaders = mutableSetOf(
        "connection",
        "content-length",
        "transfer-encoding",
        "date",
        "server",
        "keep-alive",
        "proxy-authenticate",
        "proxy-connection",
        "trailers",
        "upgrade",
        "www-authenticate"
    )

    /**
     * 转换Netty代理响应为Servlet响应
     *
     * @param response 目标Servlet响应
     * @param proxyResponse 代理响应对象
     * @throws ProxyException 转换失败异常
     */
    fun transform(response: HttpServletResponse, proxyResponse: ProxyClient.ProxyResponse) {
        // 设置响应状态码
        response.status = proxyResponse.statusCode

        // 透传Headers
        transferResponseHeaders(response, proxyResponse.headers)

        // 写入响应体
        writeResponseBody(response, proxyResponse.body)
    }

    /**
     * 透传响应Headers
     */
    private fun transferResponseHeaders(response: HttpServletResponse, headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            // 跳过需要过滤的Headers
            if (shouldFilterResponseHeader(name.lowercase())) {
                return@forEach
            }

            // 特殊处理某些Headers
            when (name.lowercase()) {
                "content-type" -> {
                    // Content-Type需要特殊处理，确保正确设置
                    response.contentType = value
                }

                "set-cookie" -> {
                    // Set-Cookie需要特殊处理，可能需要添加多个
                    response.addHeader(name, value)
                }

                else -> {
                    // 普通Header直接设置
                    response.setHeader(name, value)
                }
            }
        }
    }

    /**
     * 写入响应体
     */
    private fun writeResponseBody(response: HttpServletResponse, body: ByteArray) {
        if (body.isNotEmpty()) {
            response.outputStream.use { outputStream ->
                outputStream.write(body)
                outputStream.flush()
            }
        } else {
            log.debug("Empty response body, skipping write")
        }
    }

    /**
     * 判断是否需要过滤响应Header
     */
    private fun shouldFilterResponseHeader(headerName: String): Boolean {
        return filteredResponseHeaders.contains(headerName.lowercase())
    }
}
