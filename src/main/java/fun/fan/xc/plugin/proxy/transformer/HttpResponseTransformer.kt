package `fun`.fan.xc.plugin.proxy.transformer

import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletResponse

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
        try {
            log.debug("Transforming Netty response to Servlet response. Status: {}, BodySize: {}",
                     proxyResponse.statusCode, proxyResponse.body.size)

            // 设置响应状态码
            setResponseStatus(response, proxyResponse.statusCode)

            // 透传Headers
            transferResponseHeaders(response, proxyResponse.headers)

            // 写入响应体
            writeResponseBody(response, proxyResponse.body)

            log.debug("Response transformation completed. Status: {}", proxyResponse.statusCode)

        } catch (e: ProxyException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to transform response", e)
            throw ProxyException("Failed to transform response: ${e.message}")
        }
    }

    /**
     * 设置响应状态码
     */
    private fun setResponseStatus(response: HttpServletResponse, statusCode: Int) {
        try {
            response.status = statusCode
            log.debug("Set response status: {}", statusCode)
        } catch (e: Exception) {
            log.error("Failed to set response status: {}", statusCode, e)
            throw ProxyException("Failed to set response status: $statusCode")
        }
    }

    /**
     * 透传响应Headers
     */
    private fun transferResponseHeaders(response: HttpServletResponse, headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            try {
                // 跳过需要过滤的Headers
                if (shouldFilterResponseHeader(name.lowercase())) {
                    log.debug("Filtering response header: {}", name)
                    return@forEach
                }

                // 特殊处理某些Headers
                when (name.lowercase()) {
                    "content-type" -> {
                        // Content-Type需要特殊处理，确保正确设置
                        response.contentType = value
                        log.debug("Set Content-Type: {}", value)
                    }
                    "content-disposition" -> {
                        // Content-Disposition需要完整设置
                        response.setHeader(name, value)
                        log.debug("Set Content-Disposition: {}", value)
                    }
                    "cache-control" -> {
                        // Cache-Control需要特殊处理
                        response.setHeader(name, value)
                        log.debug("Set Cache-Control: {}", value)
                    }
                    "set-cookie" -> {
                        // Set-Cookie需要特殊处理，可能需要添加多个
                        response.addHeader(name, value)
                        log.debug("Added Set-Cookie header")
                    }
                    else -> {
                        // 普通Header直接设置
                        response.setHeader(name, value)
                        log.debug("Set header: {} = {}", name, value)
                    }
                }

            } catch (e: Exception) {
                log.warn("Failed to set response header: {} = {}", name, value, e)
                // 单个Header设置失败不应该中断整个响应过程
            }
        }
    }

    /**
     * 写入响应体
     */
    private fun writeResponseBody(response: HttpServletResponse, body: ByteArray) {
        try {
            if (body.isNotEmpty()) {
                response.outputStream.use { outputStream ->
                    outputStream.write(body)
                    outputStream.flush()
                }
                log.debug("Written response body: {} bytes", body.size)
            } else {
                log.debug("Empty response body, skipping write")
            }
        } catch (e: Exception) {
            log.error("Failed to write response body", e)
            throw ProxyException("Failed to write response body: ${e.message}")
        }
    }

    /**
     * 判断是否需要过滤响应Header
     */
    private fun shouldFilterResponseHeader(headerName: String): Boolean {
        return filteredResponseHeaders.contains(headerName.lowercase())
    }

    /**
     * 安全的Header值清理
     */
    private fun sanitizeHeaderValue(value: String): String {
        // 移除潜在的CRLF注入
        return value.replace(Regex("\\r|\\n"), "")
            .trim()
    }

    /**
     * 验证响应状态码
     */
    fun validateStatusCode(statusCode: Int): Boolean {
        return statusCode in 100..599
    }

    /**
     * 获取响应状态码的描述信息
     */
    fun getStatusDescription(statusCode: Int): String {
        return when (statusCode) {
            in 100..199 -> "Informational"
            in 200..299 -> "Success"
            in 300..399 -> "Redirection"
            in 400..499 -> "Client Error"
            in 500..599 -> "Server Error"
            else -> "Unknown"
        }
    }

    /**
     * 处理特殊的状态码情况
     */
    fun handleSpecialStatusCodes(response: HttpServletResponse, statusCode: Int) {
        when (statusCode) {
            204 -> {
                // No Content，需要确保Content-Length为0
                response.setHeader("Content-Length", "0")
            }
            304 -> {
                // Not Modified，不应该包含响应体
                response.setHeader("Content-Length", "0")
            }
            in 300..399 -> {
                // 重定向相关的处理
                if (!response.containsHeader("Location") && !response.containsHeader("location")) {
                    log.warn("Redirection status {} missing Location header", statusCode)
                }
            }
        }
    }

    /**
     * 添加安全性响应头
     */
    fun addSecurityHeaders(response: HttpServletResponse) {
        // XSS防护
        if (!response.containsHeader("X-XSS-Protection")) {
            response.setHeader("X-XSS-Protection", "1; mode=block")
        }

        // 内容类型选项
        if (!response.containsHeader("X-Content-Type-Options")) {
            response.setHeader("X-Content-Type-Options", "nosniff")
        }

        // 点击劫持防护
        if (!response.containsHeader("X-Frame-Options")) {
            response.setHeader("X-Frame-Options", "SAMEORIGIN")
        }

        // 内容安全策略
        if (!response.containsHeader("Content-Security-Policy")) {
            response.setHeader("Content-Security-Policy", "default-src 'self'")
        }
    }

    /**
     * 获取过滤的响应Headers列表（只读）
     */
    fun getFilteredResponseHeaders(): Set<String> {
        return filteredResponseHeaders.toSet()
    }

    /**
     * 添加自定义过滤响应Header
     */
    fun addFilteredResponseHeader(headerName: String): Boolean {
        return filteredResponseHeaders.add(headerName.lowercase())
    }

    /**
     * 移除自定义过滤响应Header
     */
    fun removeFilteredResponseHeader(headerName: String): Boolean {
        return filteredResponseHeaders.remove(headerName.lowercase())
    }

    /**
     * 检查响应是否包含特定Header
     */
    fun containsHeaderCaseInsensitive(response: HttpServletResponse, headerName: String): Boolean {
        val headerNameLower = headerName.lowercase()
        return response.headerNames.asSequence().any { it.lowercase() == headerNameLower }
    }
}