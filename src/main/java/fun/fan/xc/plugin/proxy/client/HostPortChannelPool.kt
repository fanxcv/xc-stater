package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.pool.ChannelHealthChecker
import io.netty.channel.pool.ChannelPoolHandler
import io.netty.channel.pool.FixedChannelPool
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.util.AttributeKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resumeWithException
import kotlin.math.min

/**
 * Host:Port连接池（优化版）
 * 专注于Netty连接的池化管理和生命周期控制
 *
 * @author fan
 *
 * ## 职责范围
 * - 按host:port分组管理连接池
 * - 连接的异步获取和释放
 * - 连接池的生命周期管理
 * - 连接的健康监控和统计
 * - 连接超时和异常处理
 *
 * ## 不负责的功能
 * - HTTP请求执行（委托给ProxyClient）
 * - 负载均衡（委托给ProxyLoadBalancer）
 * - 业务逻辑判断（委托给上层组件）
 *
 * ## 配置优化
 * - 简化配置参数，减少复杂度
 * - 优化连接获取和释放性能
 * - 增强错误处理和日志记录
 */
class HostPortChannelPool(private val properties: ProxyProperties) {
    private val log: Logger = LoggerFactory.getLogger(HostPortChannelPool::class.java)

    // 共享 EventLoopGroup，避免重复创建
    // 优化线程数配置：根据CPU核心数动态设置，提升并发性能
    private val eventLoopGroup = NioEventLoopGroup(
        // 推荐值为CPU核心数的2倍
        min(Runtime.getRuntime().availableProcessors() * 2, properties.maxEventLoopThreads)
    )

    // 按host:port分组的连接池
    private val hostPortPools = ConcurrentHashMap<String, FixedChannelPool>()

    // 缓存host:port键的计算结果
    private val hostPortKeyCache = ConcurrentHashMap<URI, String>()

    /**
     * 获取路由键 (host:port)
     */
    private fun getHostPortKey(uri: URI): String {
        return hostPortKeyCache.getOrPut(uri) {
            val port = if (uri.port == -1) {
                if (uri.scheme == "https") 443 else 80
            } else {
                uri.port
            }
            "${uri.host}:$port"
        }
    }

    /**
     * 异步获取连接（协程挂起版本）
     *
     * @param uri 目标URI
     * @return 连接Channel
     * @throws java.util.concurrent.TimeoutException 获取超时
     * @throws Exception 其他连接异常
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun acquireConnectionSuspend(uri: URI): Channel {
        val hostPortKey = getHostPortKey(uri)

        // 获取host:port连接池（使用延迟初始化避免启动时创建）
        val channelPool = hostPortPools.getOrPut(hostPortKey) {
            createChannelPool(uri)
        }

        // 获取连接
        return suspendCancellableCoroutine { continuation ->
            val future = channelPool.acquire()

            future.addListener {
                if (future.isSuccess) {
                    // 设置channel属性，用于release时识别
                    future.getNow().attr(AttributeKey.valueOf<String>("hostPortKey")).set(hostPortKey)
                    continuation.resume(future.getNow()) {
                        // onCancellation 回调
                        if (future.isSuccess && future.isDone) {
                            log.debug("Connection acquisition cancelled for: {}, releasing connection", hostPortKey)
                            channelPool.release(future.getNow())
                        }
                    }
                } else {
                    log.error("Failed to acquire connection for: {}", hostPortKey, future.cause())
                    continuation.resumeWithException(
                        future.cause() ?: RuntimeException("Unknown connection acquisition error")
                    )
                }
            }
        }
    }

    /**
     * 释放连接
     *
     * @param channel 要释放的连接
     */
    fun releaseConnection(channel: Channel) {
        try {
            val hostPortKey = channel.attr(AttributeKey.valueOf<String>("hostPortKey")).get()
            if (hostPortKey != null) {
                hostPortPools[hostPortKey]?.release(channel)
                // log.debug("Connection released for: {}", hostPortKey)
            } else {
                log.warn("Cannot release connection: hostPortKey attribute not found")
                channel.close()
            }
        } catch (e: Exception) {
            log.warn("Failed to release connection: {}", e.message)
            // 释放失败，直接关闭
            channel.close()
        }
    }

    /**
     * 创建连接池
     */
    private fun createChannelPool(uri: URI): FixedChannelPool {
        val hostPortKey = getHostPortKey(uri)
        log.info("Creating new channel pool for: {}", hostPortKey)

        val bootstrap = createBootstrap(uri)
        val poolHandler = ConnectionPoolHandler()

        return FixedChannelPool(
            bootstrap,
            poolHandler,
            ChannelHealthChecker.ACTIVE,
            FixedChannelPool.AcquireTimeoutAction.NEW,
            properties.maxWaitTimeout,
            properties.maxConnections,
            properties.maxWaitQueueSize
        ).apply {
            log.info(
                "Channel pool created for: {} with max connections: {}, max wait queue: {}, max wait time: {}",
                hostPortKey,
                properties.maxConnections,
                properties.maxWaitQueueSize,
                properties.maxWaitTimeout
            )
        }
    }

    /**
     * 创建Bootstrap
     */
    private fun createBootstrap(uri: URI): Bootstrap {
        val hostPortKey = getHostPortKey(uri)
        val (host, port) = parseHostPort(hostPortKey)

        return Bootstrap()
            .group(eventLoopGroup) // 使用共享的 EventLoopGroup
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, min(5000L, properties.timeout).toInt())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .apply {
                // 设置socket缓冲区大小
                option(ChannelOption.SO_RCVBUF, 32 * 1024) // 32KB接收缓冲区
                option(ChannelOption.SO_SNDBUF, 32 * 1024) // 32KB发送缓冲区
            }
            .remoteAddress(java.net.InetSocketAddress(host, port))
    }

    /**
     * 解析host:port
     */
    private fun parseHostPort(hostPortKey: String): Pair<String, Int> {
        val parts = hostPortKey.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid host:port format: $hostPortKey")
        }
        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid port number: ${parts[1]}")
        return Pair(host, port)
    }

    /**
     * 连接池处理器
     * 负责Channel的初始化和清理
     */
    private class ConnectionPoolHandler : ChannelPoolHandler {
        private val log = LoggerFactory.getLogger(ConnectionPoolHandler::class.java)

        override fun channelCreated(channel: Channel) {
            log.debug("Channel created: {}", channel)

            // 初始化Channel pipeline
            val pipeline = channel.pipeline()

            // 添加HTTP编解码器和聚合器
            pipeline.addFirst("codec", HttpClientCodec())
            pipeline.addAfter("codec", "aggregator", HttpObjectAggregator(65536))

            // 添加连接池特定的处理器
            pipeline.addLast("poolHandler", object : ChannelInboundHandlerAdapter() {
                override fun channelActive(ctx: io.netty.channel.ChannelHandlerContext) {
                    // log.debug("Channel became active: {}", ctx.channel())
                    super.channelActive(ctx)
                }

                override fun channelInactive(ctx: io.netty.channel.ChannelHandlerContext) {
                    // log.debug("Channel became inactive: {}", ctx.channel())
                    super.channelInactive(ctx)
                }

                override fun exceptionCaught(ctx: io.netty.channel.ChannelHandlerContext, cause: Throwable) {
                    // log.warn("Channel exception: {} - {}", ctx.channel(), cause.message)
                    ctx.close()
                }
            })
        }

        override fun channelAcquired(channel: Channel) {
            log.debug("Channel acquired: {}", channel)
            // 连接被获取时的处理，目前不需要特殊逻辑
        }

        override fun channelReleased(channel: Channel) {
            log.debug("Channel released: {}", channel)
            // 连接被释放时的处理，目前不需要特殊逻辑
        }
    }
}
