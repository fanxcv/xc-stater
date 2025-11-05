package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.handler.BufferPool
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.AttributeKey
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 代理客户端门面类
 * 提供简单的代理请求接口，内部直接处理HTTP请求执行
 *
 * @author fan
 *
 * ## 职责范围
 * - 提供统一的代理请求接口
 * - HTTP请求的发送和响应接收
 * - HTTP编解码器的动态管理
 * - 响应数据的完整收集和处理
 * - 超时控制和相关异常处理
 * - 连接生命周期的委托管理
 *
 * ## 使用示例
 * ```kotlin
 * val client = ProxyClient(connectionPool, properties)
 * val response = client.executeProxyRequest(request, "http://example.com")
 * ```
 */
class ProxyClient() {

    private val log: Logger = LoggerFactory.getLogger(ProxyClient::class.java)

    /**
     * 响应状态数据类
     * 用于存储单个HTTP请求的响应处理状态
     */
    data class ResponseState(
        val continuation: CancellableContinuation<ProxyResponse>,
        val targetUrl: String
    )

    companion object {
        private val RESPONSE_STATE_KEY = AttributeKey.valueOf<ResponseState>("responseState")
        private val handler = ProxyResponseHandler()
    }

    /**
     * 执行代理请求
     *
     * @param request HTTP请求对象
     * @param targetUrl 目标URL
     * @param connection 已获取的连接（可选）
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理响应
     * @throws java.util.concurrent.TimeoutException 请求超时
     * @throws Exception 执行失败异常
     */
    suspend fun executeProxyRequest(
        request: FullHttpRequest,
        targetUrl: String,
        connection: Channel,
        timeoutMs: Long
    ): ProxyResponse {
        return withTimeout(timeoutMs) {
            // 发送请求并等待响应
            sendRequestAndWaitResponse(connection, request, targetUrl)
        }
    }


    /**
     * 发送请求并等待响应
     */
    private suspend fun sendRequestAndWaitResponse(
        channel: Channel,
        request: FullHttpRequest,
        targetUrl: String
    ): ProxyResponse {
        return suspendCancellableCoroutine { continuation ->
            channel.writeAndFlush(request).addListener { futureListener ->
                if (futureListener.isSuccess) {
                    channel.attr(RESPONSE_STATE_KEY).set(ResponseState(continuation, targetUrl))
                    // 设置响应处理器（使用内部类实例）
                    try {
                        if (channel.pipeline().get("proxyHandler") == null) {
                            channel.pipeline().addLast("proxyHandler", handler)
                        }
                    } catch (e: Exception) {
                        log.error("Failed to add response handler: {}", e.message)
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                } else {
                    log.error("Failed to send HTTP request: {}", futureListener.cause()?.message)
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
     * 代理响应处理器
     * 负责处理HTTP响应的完整生命周期
     */
    @ChannelHandler.Sharable
    private class ProxyResponseHandler() : SimpleChannelInboundHandler<HttpObject>() {
        private val log: Logger = LoggerFactory.getLogger(ProxyResponseHandler::class.java)
        private var httpResponse: HttpResponse? = null
        private val contentBuffer = BufferPool.acquire()

        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
            val state = ctx.channel().attr(RESPONSE_STATE_KEY).get()

            try {
                when (msg) {
                    is FullHttpResponse -> {
                        // 完整的HTTP响应（包含头和体）
                        val statusCode = msg.status().code()
                        val headers = extractHeaders(msg)
                        val bodyBytes = extractBody(msg)
                        completeResponse(state, statusCode, headers, bodyBytes)
                    }

                    is HttpResponse -> {
                        // HTTP响应头（非聚合模式）
                        httpResponse = msg
                        if (msg is LastHttpContent) {
                            completeResponseFromParts(state, httpResponse, contentBuffer)
                        }
                    }

                    is HttpContent -> {
                        // HTTP响应体（非聚合模式）- 零拷贝优化
                        if (msg.content().isReadable) {
                            // 使用BufferPool.transferToZeroCopy直接传输，避免ByteArray分配
                            BufferPool.transferToZeroCopy(msg.content(), contentBuffer)
                        }
                        if (msg is LastHttpContent) {
                            completeResponseFromParts(state, httpResponse, contentBuffer)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Error handling HTTP response: {}", e.message, e)

                if (state?.continuation?.isActive ?: false) {
                    state.continuation.resumeWithException(e)
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            log.error("Exception in HTTP executor: {}", cause.message)
            val state = ctx.channel().attr(RESPONSE_STATE_KEY).get()

            if (state?.continuation?.isActive ?: false) {
                state.continuation.resumeWithException(cause)
            }
            ctx.close()
        }

        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
            if (evt is io.netty.handler.timeout.IdleStateEvent) {
                log.warn("Channel idle timeout: {}", evt.state())
                val state = ctx.channel().attr(RESPONSE_STATE_KEY).get()

                if (state?.continuation?.isActive ?: false) {
                    state.continuation.resumeWithException(TimeoutException("Channel idle timeout: ${evt.state()}"))
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
         * 提取Body - 零拷贝优化
         * 直接返回ByteBuf引用，避免ByteArray分配
         */
        private fun extractBody(response: FullHttpResponse): ByteBuf {
            return if (response.content().isReadable) {
                // 直接返回内容ByteBuf的引用，不进行拷贝
                // 读取完成后ByteBuf会被自动释放
                response.content().retainedSlice()
            } else {
                // 返回空缓冲区
                BufferPool.acquire(0)
            }
        }

        /**
         * 完成完整响应的处理 - 零拷贝优化
         */
        private fun completeResponse(
            state: ResponseState,
            statusCode: Int,
            headers: Map<String, String>,
            bodyBytes: ByteBuf
        ) {
            // 将ByteBuf转换为ByteArray（只有在这里进行一次拷贝）
            val bodyArray = ByteArray(bodyBytes.readableBytes())
            bodyBytes.getBytes(0, bodyArray)

            val proxyResponse = ProxyResponse(statusCode, headers, bodyArray, state.targetUrl)
            if (state.continuation.isActive) {
                state.continuation.resume(proxyResponse)
            }

            // 释放ByteBuf资源
            bodyBytes.release()
        }

        /**
         * 完成分片响应的处理 - 零拷贝优化
         */
        private fun completeResponseFromParts(
            state: ResponseState,
            httpResponse: HttpResponse?,
            contentBuffer: ByteBuf
        ) {
            if (!state.continuation.isActive) {
                return
            }

            val statusCode = httpResponse?.status()?.code() ?: 200

            val headers = mutableMapOf<String, String>()
            httpResponse?.headers()?.entries()?.forEach { entry ->
                headers[entry.key] = entry.value
            }

            // 直接使用contentBuffer（零拷贝），不再转换为ByteArrayOutputStream
            completeResponse(state, statusCode, headers, contentBuffer)

            // 重置contentBuffer以供下次使用（保留在池中复用）
            contentBuffer.clear()
        }

        /**
         * 处理通道关闭时的资源清理
         */
        override fun channelInactive(ctx: ChannelHandlerContext) {
            // 清理contentBuffer，归还到对象池
            if (contentBuffer.refCnt() > 0) {
                BufferPool.release(contentBuffer)
            }
            super.channelInactive(ctx)
        }
    }

    /**
     * HTTP响应数据类
     */
    data class ProxyResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: ByteArray,
        val target: String = ""
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ProxyResponse

            if (statusCode != other.statusCode) return false
            if (headers != other.headers) return false
            if (!body.contentEquals(other.body)) return false
            if (target != other.target) return false

            return true
        }

        override fun hashCode(): Int {
            var result = statusCode
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            result = 31 * result + target.hashCode()
            return result
        }
    }
}
