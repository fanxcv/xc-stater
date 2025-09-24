package `fun`.fan.xc.plugin.proxy.transformer

import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import `fun`.fan.xc.starter.utils.Dict
import io.netty.handler.codec.http.FullHttpRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

/**
 * HTTP请求转换器
 * 负责将Servlet请求转换为Netty HTTP请求，包括参数透传、消息体透传、Header透传等
 *
 * @author fan
 *
 * ## 功能特性
 * - Servlet请求到Netty请求的完整转换
 * - URL参数的完整透传
 * - 请求消息体的完整透传
 * - Header的智能透传和过滤
 * - 支持各种HTTP方法
 * - 请求规范化处理
 * - 安全性Header处理
 *
 * ## 使用示例
 * ```kotlin
 * val transformer = HttpRequestTransformer()
 * val nettyRequest = transformer.transform(servletRequest, targetUrl)
 * ```
 */
class HttpRequestTransformer {

    private val log: Logger = LoggerFactory.getLogger(HttpRequestTransformer::class.java)

    /**
     * 需要过滤的HTTP Headers列表
     * 这些Header会由Netty或者目标服务器自动处理
     */
    private val filteredHeaders = mutableSetOf(
        "content-length",
        "host",
        "connection",
        "keep-alive",
        "proxy-connection",
        "te",
        "trailers",
        "upgrade",
        "transfer-encoding"
    )

    /**
     * 转换Servlet请求为Netty HTTP请求
     *
     * @param request 原始Servlet请求
     * @param targetUri 目标服务器URL
     * @return 转换后的Netty HTTP请求
     * @throws ProxyException 转换失败异常
     */
    fun transform(request: HttpServletRequest, targetUri: URI): FullHttpRequest {
        return transform(request, targetUri, "")
    }

    /**
     * 转换Servlet请求为Netty HTTP请求
     *
     * @param request 原始Servlet请求
     * @param targetUri 目标服务器URL
     * @param pathWithinPattern 从路径模式中提取的路径部分，将追加到目标路径后面
     * @return 转换后的Netty HTTP请求
     * @throws ProxyException 转换失败异常
     */
    fun transform(request: HttpServletRequest, targetUri: URI, pathWithinPattern: String = ""): FullHttpRequest {

        // 检查是否为multipart请求
        val isMultipartRequest = isMultipartRequest(request)
        val boundary = if (isMultipartRequest) generateBoundary() else Dict.BLANK

        // 构建目标请求路径
        val targetPath = buildTargetPath(request, targetUri, pathWithinPattern)

        // 获取HTTP方法
        val httpMethod = getHttpMethod(request.method)

        // 读取请求体
        val requestBody = if (isMultipartRequest) {
            reconstructMultipartBody(request, boundary)
        } else {
            readRequestBody(request)
        }

        // 创建Netty请求
        val nettyRequest = createNettyRequest(httpMethod, targetPath, requestBody)

        // 透传Headers
        transferHeaders(request, nettyRequest)

        // 设置必要的Headers
        setRequiredHeaders(nettyRequest, requestBody, targetUri)

        if (isMultipartRequest) {
            // 特殊处理multipart的Content-Type
            nettyRequest.headers().set("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        // log.debug(
        //     "Request transformation completed. Method: {}, Path: {}, BodySize: {}",
        //     httpMethod, targetPath, requestBody.size
        // )

        return nettyRequest
    }

    /**
     * 构建目标请求路径
     */
    private fun buildTargetPath(request: HttpServletRequest, targetUri: URI, pathWithinPattern: String = ""): String {
        // 使用目标URL的路径部分
        val pathBuilder = StringBuilder(targetUri.path)

        // 如果有从路径模式中提取的路径部分，追加到目标路径后面
        if (pathWithinPattern.isNotEmpty()) {
            // 确保目标路径以/结尾，然后追加路径部分
            if (!targetUri.path.endsWith("/")) {
                pathBuilder.append("/")
            }
            // 移除路径部分开头的斜杠（如果存在），然后追加
            val normalizedPath = if (pathWithinPattern.startsWith("/")) pathWithinPattern.substring(1) else pathWithinPattern
            pathBuilder.append(normalizedPath)
        }

        // 添加查询参数
        if (request.queryString != null) {
            // 合并目标URL的查询参数和原始请求的查询参数
            val targetQuery = targetUri.query
            if (targetQuery != null) {
                pathBuilder.append("?").append(targetQuery)
                pathBuilder.append("&").append(request.queryString)
            } else {
                pathBuilder.append("?").append(request.queryString)
            }
        } else if (targetUri.query != null) {
            pathBuilder.append("?").append(targetUri.query)
        }

        return pathBuilder.toString()
    }

    /**
     * 检查是否为multipart请求
     */
    private fun isMultipartRequest(request: HttpServletRequest): Boolean {
        val contentType = request.contentType ?: return false
        return contentType.startsWith("multipart/", ignoreCase = true)
    }

    /**
     * 读取请求体
     */
    private fun readRequestBody(request: HttpServletRequest): ByteArray {
        return try {
            request.inputStream.readBytes()
        } catch (e: Exception) {
            log.warn("Failed to read request body: {}", e.message)
            ByteArray(0)
        }
    }

    /**
     * 生成boundary字符串
     */
    private fun generateBoundary(): String {
        return "----WebKitFormBoundary" + System.currentTimeMillis()
    }

    /**
     * 重构multipart请求体
     */
    private fun reconstructMultipartBody(request: HttpServletRequest, boundary: String): ByteArray {
        try {
            // 获取multipart文件
            val multipartFiles = getMultipartFiles(request)

            // 获取表单参数
            val parameterMap = request.parameterMap

            if (multipartFiles.isEmpty() && parameterMap.isEmpty()) {
                return ByteArray(0)
            }

            // 构建multipart内容
            val contentBuilder = ByteArrayOutputStream()

            // 添加表单字段
            parameterMap.forEach { (name, values) ->
                values.forEach { value ->
                    writeFormField(contentBuilder, boundary, name, value)
                }
            }

            // 添加文件字段
            multipartFiles.forEach { (name, files) ->
                files.forEach { file ->
                    writeFileField(contentBuilder, boundary, name, file)
                }
            }

            // 添加结束boundary
            contentBuilder.write("--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))

            return contentBuilder.toByteArray()
        } catch (e: Exception) {
            log.warn("Failed to reconstruct multipart body: {}", e.message)
            return ByteArray(0)
        }
    }

    /**
     * 获取multipart文件
     */
    private fun getMultipartFiles(request: HttpServletRequest): Map<String, List<MultipartFile>> {
        val files = mutableMapOf<String, List<MultipartFile>>()

        if (request is StandardMultipartHttpServletRequest) {
            val fileMap = request.multiFileMap
            fileMap.forEach { (name, fileItems) ->
                files[name] = fileItems
            }
        }

        return files
    }

    /**
     * 写入表单字段
     */
    private fun writeFormField(
        outputStream: ByteArrayOutputStream,
        boundary: String,
        name: String,
        value: String
    ) {
        val fieldHeader = "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"$name\"\r\n" +
                "\r\n"
        outputStream.write(fieldHeader.toByteArray(StandardCharsets.UTF_8))
        outputStream.write(value.toByteArray(StandardCharsets.UTF_8))
        outputStream.write("\r\n".toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * 写入文件字段
     */
    private fun writeFileField(
        outputStream: ByteArrayOutputStream,
        boundary: String,
        name: String,
        file: MultipartFile
    ) {
        try {
            val fieldHeader = "--$boundary\r\n" +
                    "Content-Disposition: form-data; name=\"$name\"; filename=\"${file.originalFilename ?: "file"}\"\r\n" +
                    "Content-Type: ${file.contentType ?: "application/octet-stream"}\r\n" +
                    "\r\n"
            outputStream.write(fieldHeader.toByteArray(StandardCharsets.UTF_8))
            outputStream.write(file.bytes)
            outputStream.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            log.warn("Failed to write file field: {}", e.message)
        }
    }

    /**
     * 获取HTTP方法
     */
    private fun getHttpMethod(method: String): io.netty.handler.codec.http.HttpMethod {
        return when (method.uppercase()) {
            "GET" -> io.netty.handler.codec.http.HttpMethod.GET
            "POST" -> io.netty.handler.codec.http.HttpMethod.POST
            "PUT" -> io.netty.handler.codec.http.HttpMethod.PUT
            "DELETE" -> io.netty.handler.codec.http.HttpMethod.DELETE
            "PATCH" -> io.netty.handler.codec.http.HttpMethod.PATCH
            "OPTIONS" -> io.netty.handler.codec.http.HttpMethod.OPTIONS
            "HEAD" -> io.netty.handler.codec.http.HttpMethod.HEAD
            "TRACE" -> io.netty.handler.codec.http.HttpMethod.TRACE
            else -> {
                log.warn("Unsupported HTTP method: {}, defaulting to GET", method)
                io.netty.handler.codec.http.HttpMethod.GET
            }
        }
    }

    /**
     * 创建Netty请求对象
     */
    private fun createNettyRequest(
        method: io.netty.handler.codec.http.HttpMethod,
        path: String,
        body: ByteArray
    ): FullHttpRequest {
        val contentBuffer = if (body.isNotEmpty()) {
            io.netty.buffer.Unpooled.wrappedBuffer(body)
        } else {
            io.netty.buffer.Unpooled.EMPTY_BUFFER
        }

        return io.netty.handler.codec.http.DefaultFullHttpRequest(
            io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
            method,
            path,
            contentBuffer
        )
    }

    /**
     * 透传Headers
     */
    private fun transferHeaders(
        request: HttpServletRequest,
        nettyRequest: FullHttpRequest
    ) {
        val headerNames = request.headerNames

        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            val headerValues = request.getHeaders(headerName)

            // 跳过需要过滤的Headers
            if (shouldFilterHeader(headerName.lowercase())) {
                continue
            }

            // 添加Header到Netty请求
            while (headerValues.hasMoreElements()) {
                val headerValue = headerValues.nextElement()
                nettyRequest.headers().add(headerName, headerValue)
            }
        }
    }

    /**
     * 设置必要的Headers
     */
    private fun setRequiredHeaders(
        nettyRequest: FullHttpRequest,
        requestBody: ByteArray,
        targetUri: URI
    ) {
        // 设置Host Header
        val port = if (targetUri.port == -1) {
            if (targetUri.scheme == "https") 443 else 80
        } else {
            targetUri.port
        }
        val hostHeader = "${targetUri.host}:$port"
        nettyRequest.headers().set("Host", hostHeader)

        // 设置Content-Length Header
        nettyRequest.headers().set("Content-Length", requestBody.size.toString())

        // 设置User-Agent Header
        if (!nettyRequest.headers().contains("User-Agent")) {
            nettyRequest.headers().set("User-Agent", "Xc-Proxy/1.0")
        }

        // 设置Connection Header（为HTTP/1.1保持连接）
        if (!nettyRequest.headers().contains("Connection")) {
            nettyRequest.headers().set("Connection", "keep-alive")
        }

        // 设置Accept Encoding Header（如果不存在）
        if (!nettyRequest.headers().contains("Accept-Encoding")) {
            nettyRequest.headers().set("Accept-Encoding", "gzip, deflate")
        }
    }

    /**
     * 判断是否需要过滤Header
     */
    private fun shouldFilterHeader(headerName: String): Boolean {
        return filteredHeaders.contains(headerName.lowercase())
    }
}
