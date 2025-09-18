package `fun`.fan.xc.plugin.proxy.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 代理客户端实现
 * 封装HTTP请求的代理转发，支持参数透传
 *
 * @author fan
 *
 * ## 功能特性
 * - 基于Netty的异步HTTP客户端
 * - 连接池管理，提高连接复用率
 * - 超时控制，防止请求长时间阻塞
 * - 支持HTTP/HTTPS协议
 * - 自动处理请求/响应头
 *
 * ## 使用示例
 * ```kotlin
 * val client = ProxyClient()
 * val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/api/test")
 * val response = client.executeProxyRequest(request, "http://example.com").get()
 * ```
 */
open class ProxyClient(private val connectionPool: NettyConnectionPool) {

    private val log: Logger = LoggerFactory.getLogger(ProxyClient::class.java)

    companion object {}

    /**
     * 执行HTTP代理请求
     *
     * @param request 原始HTTP请求
     * @param targetUrl 目标URL
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理响应
     */
    suspend fun executeProxyRequest(
        request: FullHttpRequest,
        targetUrl: String,
        timeoutMs: Long = 5000
    ): ProxyResponse {
        val startTime = System.currentTimeMillis()
        return withTimeout(timeoutMs) {
            try {
                val uri = URI(targetUrl)
                log.debug("Executing proxy request to: {}", targetUrl)

                // 获取连接
                val channel = connectionPool.acquireConnectionSuspend(uri)

                // 构建代理请求
                val proxyRequest = buildProxyRequest(request, uri)

                // 发送请求并等待响应
                suspendCancellableCoroutine<ProxyResponse> { continuation ->
                    var handlerAdded = false
                    
                    continuation.invokeOnCancellation {
                        // 清理处理器和释放连接
                        try {
                            if (handlerAdded && channel.pipeline().get("proxyHandler") != null) {
                                channel.pipeline().remove("proxyHandler")
                            }
                        } catch (e: Exception) {
                            log.debug("Failed to remove proxyHandler on cancellation: {}", e.message)
                        }
                        releaseConnection(channel)
                    }

                    channel.writeAndFlush(proxyRequest).addListener { futureListener ->
                        if (futureListener.isSuccess) {
                            log.debug("Proxy request sent successfully to: {}", targetUrl)

                            // 设置响应处理器
                            if (channel.pipeline().get("proxyHandler") != null) {
                                try {
                                    channel.pipeline().remove("proxyHandler")
                                } catch (e: Exception) {
                                    log.debug("Failed to remove existing proxyHandler: {}", e.message)
                                }
                            }

                            val handler = object : SimpleChannelInboundHandler<HttpObject>() {
                                private var httpResponse: HttpResponse? = null
                                private val contentBuffer = mutableListOf<ByteArray>()
                                
                                override fun channelRead0(ctx: io.netty.channel.ChannelHandlerContext, msg: HttpObject) {
                                    try {
                                        log.debug("Received HTTP object: {}", msg.javaClass.simpleName)
                                        when (msg) {
                                            is FullHttpResponse -> {
                                                // 完整的HTTP响应（包含头和体）- 这是HttpObjectAggregator聚合后的结果
                                                log.debug("Received FullHttpResponse with status: {}", msg.status())
                                                
                                                val statusCode = msg.status().code()
                                                val headers = mutableMapOf<String, String>()
                                                msg.headers().forEach { entry ->
                                                    headers[entry.key] = entry.value
                                                }
                                                
                                                val bodyBytes = if (msg.content().isReadable) {
                                                    val bytes = ByteArray(msg.content().readableBytes())
                                                    msg.content().readBytes(bytes)
                                                    log.debug("Read {} bytes from FullHttpResponse content", bytes.size)
                                                    bytes
                                                } else {
                                                    log.debug("No readable content in FullHttpResponse")
                                                    ByteArray(0)
                                                }
                                                
                                                val proxyResponse = ProxyResponse(statusCode, headers, bodyBytes)
                                                
                                                // 清理处理器
                                                try {
                                                    channel.pipeline().remove(this)
                                                } catch (e: Exception) {
                                                    log.debug("Failed to remove proxyHandler: {}", e.message)
                                                }
                                                
                                                // 释放连接
                                                releaseConnection(channel)
                                                
                                                // 记录请求处理时间
                                                val endTime = System.currentTimeMillis()
                                                val duration = endTime - startTime
                                                log.info(
                                                    "Proxy request to {} completed with status {} in {}ms, body size: {} bytes",
                                                    targetUrl,
                                                    proxyResponse.statusCode,
                                                    duration,
                                                    bodyBytes.size
                                                )
                                                
                                                if (continuation.isActive) {
                                                    continuation.resume(proxyResponse)
                                                }
                                            }
                                            is HttpResponse -> {
                                                // HTTP响应头（非聚合模式）
                                                log.debug("Received HttpResponse with status: {}", msg.status())
                                                httpResponse = msg
                                                if (msg is LastHttpContent) {
                                                    completeResponse(msg, continuation)
                                                }
                                            }
                                            is HttpContent -> {
                                                // HTTP响应体（非聚合模式）
                                                log.debug("Received HttpContent, readable: {}", msg.content().isReadable)
                                                if (msg.content().isReadable) {
                                                    val bytes = ByteArray(msg.content().readableBytes())
                                                    msg.content().readBytes(bytes)
                                                    contentBuffer.add(bytes)
                                                    log.debug("Added {} bytes to content buffer", bytes.size)
                                                }
                                                if (msg is LastHttpContent) {
                                                    log.debug("Received LastHttpContent, completing response")
                                                    completeResponse(msg, continuation)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        log.error("Error handling proxy response: {}", e.message, e)
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(e)
                                        }
                                    }
                                }
                                
                                private fun completeResponse(@Suppress("UNUSED_PARAMETER") lastContent: LastHttpContent, cont: CancellableContinuation<ProxyResponse>) {
                                    if (!cont.isActive) {
                                        log.debug("Continuation is not active, skipping response completion")
                                        return
                                    }
                                    
                                    val response = httpResponse
                                    val statusCode = response?.status()?.code() ?: 200
                                    
                                    log.debug("Completing response with status: {}, content buffer size: {}", statusCode, contentBuffer.size)
                                    
                                    val headers = mutableMapOf<String, String>()
                                    response?.headers()?.forEach { entry ->
                                        headers[entry.key] = entry.value
                                    }
                                    
                                    val bodyBytes = if (contentBuffer.isNotEmpty()) {
                                        val totalSize = contentBuffer.sumOf { it.size }
                                        log.debug("Combining {} content chunks, total size: {} bytes", contentBuffer.size, totalSize)
                                        contentBuffer.reduce { acc, bytes -> acc + bytes }
                                    } else {
                                        log.debug("No content in buffer, using empty body")
                                        ByteArray(0)
                                    }
                                    
                                    val proxyResponse = ProxyResponse(statusCode, headers, bodyBytes)
                                    
                                    // 清理处理器
                                    try {
                                        channel.pipeline().remove(this)
                                    } catch (e: Exception) {
                                        log.debug("Failed to remove proxyHandler: {}", e.message)
                                    }
                                    
                                    // 释放连接
                                    releaseConnection(channel)
                                    
                                    // 记录请求处理时间
                                    val endTime = System.currentTimeMillis()
                                    val duration = endTime - startTime
                                    log.info(
                                        "Proxy request to {} completed with status {} in {}ms, body size: {} bytes",
                                        targetUrl,
                                        proxyResponse.statusCode,
                                        duration,
                                        bodyBytes.size
                                    )
                                    
                                    cont.resume(proxyResponse)
                                }

                                override fun exceptionCaught(ctx: io.netty.channel.ChannelHandlerContext, cause: Throwable) {
                                    log.error("Exception in proxy client: {}", cause.message)
                                    if (continuation.isActive) {
                                        continuation.resumeWithException(cause)
                                    }
                                    ctx.close()
                                }

                                override fun userEventTriggered(ctx: io.netty.channel.ChannelHandlerContext, evt: Any) {
                                    if (evt is io.netty.handler.timeout.IdleStateEvent) {
                                        log.warn("Channel idle timeout: {}", evt.state())
                                        if (continuation.isActive) {
                                            continuation.resumeWithException(TimeoutException("Channel idle timeout: ${evt.state()}"))
                                        }
                                        ctx.close()
                                    } else {
                                        super.userEventTriggered(ctx, evt)
                                    }
                                }
                            }
                            
                            channel.pipeline().addLast("proxyHandler", handler)
                            handlerAdded = true
                        } else {
                            log.error("Failed to send proxy request: {}", futureListener.cause()?.message)
                            releaseConnection(channel)
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    futureListener.cause() ?: RuntimeException("Failed to send proxy request")
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to execute proxy request: {}", e.message)
                throw e
            }
        }
    }

    /**
     * 构建代理请求
     */
    private fun buildProxyRequest(request: FullHttpRequest, targetUri: URI): FullHttpRequest {
        // 构建目标URL路径和查询参数
        val pathBuilder = StringBuilder(targetUri.path)
        if (targetUri.query != null) {
            pathBuilder.append("?").append(targetUri.query)
        }

        // 复制请求体
        val content = if (request.content().isReadable) {
            val bytes = ByteArray(request.content().readableBytes())
            request.content().readBytes(bytes)
            bytes
        } else {
            ByteArray(0)
        }

        // 创建代理请求
        val proxyRequest = DefaultFullHttpRequest(
            request.protocolVersion(),
            request.method(),
            pathBuilder.toString(),
            Unpooled.wrappedBuffer(content)
        )

        // 复制headers
        request.headers().forEach { entry ->
            val key = entry.key
            val value = entry.value

            // 过滤掉需要特殊处理的headers
            when (key.lowercase()) {
                "host" -> {
                    // 更新Host header为目标服务器
                    val port = if (targetUri.port == -1) {
                        if (targetUri.scheme == "https") 443 else 80
                    } else {
                        targetUri.port
                    }
                    proxyRequest.headers().set("Host", "${targetUri.host}:$port")
                }

                "content-length" -> {
                    // 重新计算content-length
                    proxyRequest.headers().set("Content-Length", content.size.toString())
                }

                "connection", "keep-alive", "proxy-connection", "te", "trailers", "upgrade" -> {
                    // 跳过连接相关的headers，让Netty自动处理
                }

                else -> {
                    // 复制其他headers
                    proxyRequest.headers().add(key, value)
                }
            }
        }

        // 设置User-Agent
        if (!proxyRequest.headers().contains("User-Agent")) {
            proxyRequest.headers().set("User-Agent", "Xc-Proxy/1.0")
        }

        log.debug(
            "Built proxy request: {} {} with {} headers",
            proxyRequest.method(), proxyRequest.uri(), proxyRequest.headers().size()
        )

        return proxyRequest
    }





    /**
     * 释放连接
     *
     * @param channel 连接Channel
     */
    private fun releaseConnection(channel: Channel) {
        try {
            // 从pipeline中移除IdleStateHandler，避免连接池中的连接继续被监控
            if (channel.pipeline().get("idle") != null) {
                channel.pipeline().remove("idle")
            }
            // 返回连接池
            connectionPool.releaseConnection(channel)
        } catch (e: Exception) {
            log.warn("Failed to release connection: {}", e.message)
        }
    }

    /**
     * 关闭客户端
     */
    fun shutdown() {
        log.info("Shutting down proxy client...")
        connectionPool.shutdown()
        log.info("Proxy client shutdown completed")
    }

    /**
     * 代理响应
     */
    data class ProxyResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProxyResponse) return false
            return statusCode == other.statusCode &&
                    headers == other.headers &&
                    body.contentEquals(other.body)
        }

        override fun hashCode(): Int {
            var result = statusCode
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            return result
        }
    }
}
