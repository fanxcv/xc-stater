package `fun`.fan.xc.plugin.proxy.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.TimeoutException
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
open class ProxyClient(private val connectionPool: HostPortChannelPool) {

    private val log: Logger = LoggerFactory.getLogger(ProxyClient::class.java)

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

                // 获取连接
                val channel = connectionPool.acquireConnectionSuspend(uri)

                // 动态添加HTTP编解码器到pipeline
                if (channel.pipeline().get("codec") == null) {
                    channel.pipeline().addFirst("codec", io.netty.handler.codec.http.HttpClientCodec())
                    channel.pipeline().addAfter("codec", "aggregator", io.netty.handler.codec.http.HttpObjectAggregator(65536))
                }

                // 构建代理请求
                val proxyRequest = buildProxyRequest(request, uri)

                // 发送请求并等待响应
                suspendCancellableCoroutine { continuation ->
                    var handlerAdded = false

                    continuation.invokeOnCancellation {
                        // 清理处理器和释放连接
                        try {
                            if (handlerAdded && channel.pipeline().get("proxyHandler") != null) {
                                channel.pipeline().remove("proxyHandler")
                            }
                        } catch (_: Exception) {
                            // 忽略清理异常
                        }
                        releaseConnection(channel)
                    }

                    // 由于Channel已经有HTTP编解码器，直接发送HTTP请求对象
                    channel.writeAndFlush(proxyRequest).addListener { futureListener ->
                        if (futureListener.isSuccess) {
                            // 设置响应处理器
                            if (channel.pipeline().get("proxyHandler") != null) {
                                try {
                                    channel.pipeline().remove("proxyHandler")
                                } catch (_: Exception) {
                                    // 忽略清理异常
                                }
                            }

                            val handler = object : SimpleChannelInboundHandler<HttpObject>() {
                                private var httpResponse: HttpResponse? = null
                                private val contentBuffer = mutableListOf<ByteArray>()

                                override fun channelRead0(
                                    ctx: io.netty.channel.ChannelHandlerContext,
                                    msg: HttpObject
                                ) {
                                    try {
                                        when (msg) {
                                            is FullHttpResponse -> {
                                                // 完整的HTTP响应（包含头和体）
                                                val statusCode = msg.status().code()
                                                val headers = mutableMapOf<String, String>()
                                                msg.headers().forEach { entry ->
                                                    headers[entry.key] = entry.value
                                                }

                                                val bodyBytes = if (msg.content().isReadable) {
                                                    val bytes = ByteArray(msg.content().readableBytes())
                                                    msg.content().readBytes(bytes)
                                                    bytes
                                                } else {
                                                    ByteArray(0)
                                                }

                                                completeProxyResponse(
                                                    channel, statusCode, headers, bodyBytes, targetUrl, startTime,
                                                    continuation, this
                                                )
                                            }

                                            is HttpResponse -> {
                                                // HTTP响应头（非聚合模式）
                                                httpResponse = msg
                                                if (msg is LastHttpContent) {
                                                    completeResponse(msg, continuation)
                                                }
                                            }

                                            is HttpContent -> {
                                                // HTTP响应体（非聚合模式）
                                                if (msg.content().isReadable) {
                                                    val bytes = ByteArray(msg.content().readableBytes())
                                                    msg.content().readBytes(bytes)
                                                    contentBuffer.add(bytes)
                                                }
                                                if (msg is LastHttpContent) {
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

                                private fun completeResponse(
                                    @Suppress("UNUSED_PARAMETER") lastContent: LastHttpContent,
                                    cont: CancellableContinuation<ProxyResponse>
                                ) {
                                    if (!cont.isActive) {
                                        return
                                    }

                                    val response = httpResponse
                                    val statusCode = response?.status()?.code() ?: 200

                                    val headers = mutableMapOf<String, String>()
                                    response?.headers()?.forEach { entry ->
                                        headers[entry.key] = entry.value
                                    }

                                    val bodyBytes = if (contentBuffer.isNotEmpty()) {
                                        contentBuffer.reduce { acc, bytes -> acc + bytes }
                                    } else {
                                        ByteArray(0)
                                    }

                                    completeProxyResponse(
                                        channel, statusCode, headers, bodyBytes, targetUrl, startTime,
                                        cont, this
                                    )
                                }

                                override fun exceptionCaught(
                                    ctx: io.netty.channel.ChannelHandlerContext,
                                    cause: Throwable
                                ) {
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


        return proxyRequest
    }

    /**
     * 完成代理响应处理（提取的公共方法）
     */
    private fun completeProxyResponse(
        channel: Channel,
        statusCode: Int,
        headers: Map<String, String>,
        bodyBytes: ByteArray,
        targetUrl: String,
        startTime: Long,
        continuation: CancellableContinuation<ProxyResponse>,
        handler: SimpleChannelInboundHandler<HttpObject>
    ) {
        val proxyResponse = ProxyResponse(statusCode, headers, bodyBytes)

        // 清理处理器
        try {
            channel.pipeline().remove(handler)
        } catch (_: Exception) {
            // 忽略清理异常
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
            log.warn("Connection release failed: {}", e.message)
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
