package `fun`.fan.xc.plugin.proxy.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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
open class ProxyClient(private val connectionPool: NettyConnectionPool = NettyConnectionPool()) {

    private val log: Logger = LoggerFactory.getLogger(ProxyClient::class.java)

    // 全局复用的超时处理线程池
    companion object {
        private val timeoutExecutor: ScheduledExecutorService by lazy {
            Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(),
                { r ->
                    val thread = Thread(r, "ProxyClient-Timeout")
                    thread.isDaemon = true
                    thread
                }
            )
        }
    }

    /**
     * 执行HTTP代理请求
     *
     * @param request 原始HTTP请求
     * @param targetUrl 目标URL
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理响应
     */
    fun executeProxyRequest(
        request: FullHttpRequest,
        targetUrl: String,
        timeoutMs: Long = 5000
    ): CompletableFuture<ProxyResponse> {
        val startTime = System.currentTimeMillis()
        return try {
            val uri = URI(targetUrl)
            log.debug("Executing proxy request to: {}", targetUrl)

            // 获取连接
            val channel = connectionPool.acquireConnection(uri)

            // 构建代理请求
            val proxyRequest = buildProxyRequest(request, uri)

            // 发送请求
            val future = CompletableFuture<ProxyResponse>()

            // 添加超时处理
            val timeoutTask = timeoutExecutor.schedule({
                if (!future.isDone) {
                    log.warn("Proxy request timeout after {}ms: {}", timeoutMs, targetUrl)
                    future.completeExceptionally(TimeoutException("Proxy request timeout after ${timeoutMs}ms"))
                }
            }, timeoutMs, TimeUnit.MILLISECONDS)

            channel.writeAndFlush(proxyRequest).addListener { futureListener ->
                if (futureListener.isSuccess) {
                    log.debug("Proxy request sent successfully to: {}", targetUrl)

                    // 设置响应处理器
                    // 检查是否已经存在同名处理器，如果存在则先移除
                    if (channel.pipeline().get("proxyHandler") != null) {
                        try {
                            channel.pipeline().remove("proxyHandler")
                        } catch (e: Exception) {
                            log.debug("Failed to remove existing proxyHandler: {}", e.message)
                        }
                    }

                    channel.pipeline().addLast("proxyHandler", object : SimpleChannelInboundHandler<HttpObject>() {
                        override fun channelRead0(ctx: io.netty.channel.ChannelHandlerContext, msg: HttpObject) {
                            try {
                                when (msg) {
                                    is HttpResponse -> {
                                        handleHttpResponse(msg, future)
                                    }

                                    is HttpContent -> {
                                        handleHttpContent(msg, future)
                                    }

                                    is LastHttpContent -> {
                                        handleLastHttpContent(msg, future)
                                        // 清理处理器
                                        try {
                                            ctx.pipeline().remove(this)
                                        } catch (e: Exception) {
                                            log.debug("Failed to remove proxyHandler: {}", e.message)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                log.error("Error handling proxy response: {}", e.message)
                                if (!future.isDone) {
                                    future.completeExceptionally(e)
                                }
                            }
                        }

                        override fun exceptionCaught(ctx: io.netty.channel.ChannelHandlerContext, cause: Throwable) {
                            log.error("Exception in proxy client: {}", cause.message)
                            if (!future.isDone) {
                                future.completeExceptionally(cause)
                            }
                            ctx.close()
                        }

                        override fun userEventTriggered(ctx: io.netty.channel.ChannelHandlerContext, evt: Any) {
                            if (evt is io.netty.handler.timeout.IdleStateEvent) {
                                log.warn("Channel idle timeout: {}", evt.state())
                                if (!future.isDone) {
                                    future.completeExceptionally(TimeoutException("Channel idle timeout: ${evt.state()}"))
                                }
                                // 确保连接被正确关闭
                                ctx.close()
                            } else {
                                super.userEventTriggered(ctx, evt)
                            }
                        }
                    })
                } else {
                    log.error("Failed to send proxy request: {}", futureListener.cause()?.message)
                    if (!future.isDone) {
                        future.completeExceptionally(
                            futureListener.cause() ?: RuntimeException("Failed to send proxy request")
                        )
                    }
                }
            }

            // 设置处理完成后的回调
            future.whenComplete { response, throwable ->
                // 取消超时任务
                timeoutTask.cancel(false)
                // 确保连接被正确释放回连接池
                releaseConnection(channel)

                // 记录请求处理时间
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                if (throwable != null) {
                    log.warn("Proxy request to {} failed after {}ms: {}", targetUrl, duration, throwable.message)
                } else if (response != null) {
                    log.info("Proxy request to {} completed with status {} in {}ms", targetUrl, response.statusCode, duration)
                }
            }

            future
        } catch (e: Exception) {
            log.error("Failed to execute proxy request: {}", e.message)
            val future = CompletableFuture<ProxyResponse>()
            future.completeExceptionally(e)
            future
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
     * 处理HTTP响应
     */
    private fun handleHttpResponse(response: HttpResponse, future: CompletableFuture<ProxyResponse>) {
        log.debug("Received HTTP response: {}", response.status())

        // 防止重复完成
        if (future.isDone) {
            return
        }

        // 复制headers
        val headers = mutableMapOf<String, String>()
        response.headers().forEach { entry ->
            headers[entry.key] = entry.value
        }

        // 处理重定向
        if (isRedirect(response.status().code())) {
            val location = response.headers().get("Location")
            if (location != null) {
                log.debug("Redirecting to: {}", location)
                // 这里应该实现重定向逻辑，但为了简化，直接返回3xx响应
                future.complete(ProxyResponse(response.status().code(), headers, ByteArray(0)))
                return
            }
        }

        // 对于非重定向响应，如果已经是LastHttpContent，则直接完成future
        if (response is LastHttpContent) {
            // 获取响应体
            val bodyBytes = if (response.content().isReadable) {
                val bytes = ByteArray(response.content().readableBytes())
                response.content().readBytes(bytes)
                bytes
            } else {
                ByteArray(0)
            }

            future.complete(ProxyResponse(response.status().code(), headers, bodyBytes))
        }
        // 对于非LastHttpContent的响应，继续等待响应体
    }

    /**
     * 处理HTTP内容
     */
    private fun handleHttpContent(content: HttpContent, future: CompletableFuture<ProxyResponse>) {
        // 内容处理逻辑会在LastHttpContent中统一处理
        // 但如果content已经是LastHttpContent，则直接处理
        if (content is LastHttpContent) {
            handleLastHttpContent(content, future)
        }
    }

    /**
     * 处理最后的HTTP内容
     */
    private fun handleLastHttpContent(content: LastHttpContent, future: CompletableFuture<ProxyResponse>) {
        log.debug("Received last HTTP content")

        // 防止重复完成
        if (future.isDone) {
            return
        }

        // 获取响应体
        val bodyBytes = if (content.content().isReadable) {
            val bytes = ByteArray(content.content().readableBytes())
            content.content().readBytes(bytes)
            bytes
        } else {
            ByteArray(0)
        }

        // 状态码默认为200
        val statusCode = if (content is HttpResponse) {
            content.status().code()
        } else {
            200
        }

        // 获取headers
        val headers = mutableMapOf<String, String>()
        if (content is HttpResponse) {
            content.headers().forEach { entry ->
                headers[entry.key] = entry.value
            }
        }

        future.complete(ProxyResponse(statusCode, headers, bodyBytes))
    }

    /**
     * 判断是否为重定向状态码
     */
    private fun isRedirect(statusCode: Int): Boolean {
        return statusCode in 300..399
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
