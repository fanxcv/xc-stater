package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
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
 * 代理HTTP执行器
 * 专注于HTTP请求的执行和响应的处理，不涉及连接管理和请求构建
 *
 * @author fan
 *
 * ## 职责范围
 * - HTTP请求的发送和响应接收
 * - HTTP编解码器的动态管理
 * - 响应数据的完整收集和处理
 * - 超时控制和相关异常处理
 * - 连接生命周期的委托管理
 */
class ProxyHttpExecutor(
    private val connectionPool: HostPortChannelPool,
    private val properties: ProxyProperties
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyHttpExecutor::class.java)

    /**
     * 执行HTTP请求并获取响应
     */
    suspend fun executeHttpRequest(
        request: FullHttpRequest,
        targetUrl: String,
        connection: Channel? = null,
        timeoutMs: Long = properties.timeout.toLong()
    ): ProxyResponse {
        val startTime = System.currentTimeMillis()
        val uri = URI(targetUrl)
        return withTimeout(timeoutMs) {
            try {
                executeInternalHttpRequest(request, uri, targetUrl, connection, startTime)
            } catch (e: Exception) {
                log.error("Failed to execute HTTP request to {}: {}", targetUrl, e.message)
                throw e
            }
        }
    }

    /**
     * 内部HTTP请求执行
     */
    private suspend fun executeInternalHttpRequest(
        request: FullHttpRequest,
        uri: URI,
        targetUrl: String,
        connection: Channel? = null,
        startTime: Long
    ): ProxyResponse {
        // 使用传入的连接或获取新连接
        val channel = connection ?: connectionPool.acquireConnectionSuspend(uri)

        // 配置HTTP编解码器
        configureHttpPipeline(channel)

        try {
            // 发送请求并等待响应
            return sendRequestAndWaitResponse(channel, request, targetUrl, startTime)
        } catch (e: Exception) {
            // 只有在没有传入连接时才释放连接，避免重复释放
            if (connection == null) {
                releaseConnection(channel)
            }
            throw e
        }
    }

    /**
     * 配置HTTP编解码器
     */
    private fun configureHttpPipeline(channel: Channel) {
        if (channel.pipeline().get("codec") == null) {
            channel.pipeline().addFirst("codec", HttpClientCodec())
            channel.pipeline().addAfter("codec", "aggregator", HttpObjectAggregator(65536))
        }
    }

    /**
     * 发送请求并等待响应
     */
    private suspend fun sendRequestAndWaitResponse(
        channel: Channel,
        request: FullHttpRequest,
        targetUrl: String,
        startTime: Long
    ): ProxyResponse {
        return suspendCancellableCoroutine { continuation ->
            var handlerAdded = false

            continuation.invokeOnCancellation {
                cleanupHandler(channel, handlerAdded)
                releaseConnection(channel)
            }

            channel.writeAndFlush(request).addListener { futureListener ->
                if (futureListener.isSuccess) {
                    // 设置响应处理器
                    val handler = ProxyResponseHandler(continuation, channel, targetUrl, startTime)
                    try {
                        channel.pipeline().addLast("proxyHandler", handler)
                        handlerAdded = true
                    } catch (e: Exception) {
                        log.error("Failed to add response handler: {}", e.message)
                        releaseConnection(channel)
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                } else {
                    log.error("Failed to send HTTP request: {}", futureListener.cause()?.message)
                    releaseConnection(channel)
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            futureListener.cause() ?: RuntimeException("Failed to send HTTP request")
                        )
                    }
                }
            }
        }
    }

    /**
     * 清理处理器
     */
    private fun cleanupHandler(channel: Channel, handlerAdded: Boolean) {
        try {
            if (handlerAdded && channel.pipeline().get("proxyHandler") != null) {
                channel.pipeline().remove("proxyHandler")
            }
        } catch (_: Exception) {
            // 忽略清理异常
        }
    }

    /**
     * 记录请求统计信息
     */
    private fun logRequestStats(targetUrl: String, statusCode: Int, startTime: Long, bodySize: Int) {
        val duration = System.currentTimeMillis() - startTime
        log.info(
            "HTTP request to {} completed with status {} in {}ms, response size: {} bytes",
            targetUrl,
            statusCode,
            duration,
            bodySize
        )
    }

    /**
     * 释放连接（委托给连接池）
     */
    private fun releaseConnection(channel: Channel) {
        try {
            removeIdleHandler(channel)
            connectionPool.releaseConnection(channel)
        } catch (e: Exception) {
            log.warn("Connection release failed: {}", e.message)
        }
    }

    /**
     * 移除空闲状态处理器
     */
    private fun removeIdleHandler(channel: Channel) {
        try {
            if (channel.pipeline().get("idle") != null) {
                channel.pipeline().remove("idle")
            }
        } catch (e: Exception) {
            log.debug("Failed to remove idle handler: {}", e.message)
        }
    }

    /**
     * 关闭执行器
     */
    fun shutdown() {
        log.info("Shutting down HTTP executor...")
        connectionPool.shutdown()
        log.info("HTTP executor shutdown completed")
    }

    /**
     * 代理响应处理器
     * 负责处理HTTP响应的完整生命周期
     */
    private inner class ProxyResponseHandler(
        private val continuation: CancellableContinuation<ProxyResponse>,
        private val channel: Channel,
        private val targetUrl: String,
        private val startTime: Long
    ) : SimpleChannelInboundHandler<HttpObject>() {

        private var httpResponse: HttpResponse? = null
        private val contentBuffer = mutableListOf<ByteArray>()

        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
            try {
                when (msg) {
                    is FullHttpResponse -> {
                        // 完整的HTTP响应（包含头和体）
                        val statusCode = msg.status().code()
                        val headers = extractHeaders(msg)
                        val bodyBytes = extractBody(msg)
                        completeResponse(statusCode, headers, bodyBytes)
                    }

                    is HttpResponse -> {
                        // HTTP响应头（非聚合模式）
                        httpResponse = msg
                        if (msg is LastHttpContent) {
                            completeResponseFromParts()
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
                            completeResponseFromParts()
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Error handling HTTP response: {}", e.message, e)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.error("Exception in HTTP executor: {}", cause.message)
            if (continuation.isActive) {
                continuation.resumeWithException(cause)
            }
            ctx.close()
        }

        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
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

        /**
         * 提取Headers
         */
        private fun extractHeaders(response: FullHttpResponse): Map<String, String> {
            val headers = mutableMapOf<String, String>()
            response.headers().forEach { entry ->
                headers[entry.key] = entry.value
            }
            return headers
        }

        /**
         * 提取Body
         */
        private fun extractBody(response: FullHttpResponse): ByteArray {
            return if (response.content().isReadable) {
                val bytes = ByteArray(response.content().readableBytes())
                response.content().readBytes(bytes)
                bytes
            } else {
                ByteArray(0)
            }
        }

        /**
         * 完成完整响应的处理
         */
        private fun completeResponse(statusCode: Int, headers: Map<String, String>, bodyBytes: ByteArray) {
            val proxyResponse = ProxyResponse(statusCode, headers, bodyBytes)

            cleanupHandler(channel, true)
            releaseConnection(channel)
            logRequestStats(targetUrl, proxyResponse.statusCode, startTime, bodyBytes.size)

            if (continuation.isActive) {
                continuation.resume(proxyResponse)
            }
        }

        /**
         * 完成分片响应的处理
         */
        private fun completeResponseFromParts() {
            if (!continuation.isActive) {
                return
            }

            val response = httpResponse
            val statusCode = response?.status()?.code() ?: 200

            val headers = mutableMapOf<String, String>()
            response?.headers()?.entries()?.forEach { entry ->
                headers[entry.key] = entry.value
            }

            val bodyBytes = if (contentBuffer.isNotEmpty()) {
                contentBuffer.reduce { acc, bytes -> acc + bytes }
            } else {
                ByteArray(0)
            }

            completeResponse(statusCode, headers, bodyBytes)
        }
    }

    /**
     * HTTP响应数据类
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
