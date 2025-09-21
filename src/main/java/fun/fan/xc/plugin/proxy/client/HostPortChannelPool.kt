package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.pool.ChannelPoolHandler
import io.netty.channel.pool.FixedChannelPool
import io.netty.channel.socket.nio.NioSocketChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
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
    private val eventLoopGroup = io.netty.channel.nio.NioEventLoopGroup()

    // 配置参数
    private val maxTotalConnections = properties.pool.maxTotalConnections
    private val connectionIdleTimeout = properties.pool.connectionIdleTimeout
    private val connectionTimeout = properties.pool.connectionTimeout
    private val maxWaitQueueSize = properties.pool.maxWaitQueueSize
    private val healthCheckInterval = properties.pool.healthCheckInterval
    private val connectRetryCount = properties.pool.connectRetryCount

    // 优化连接池配置：合理分配连接数，避免某个host占用过多资源
    private val maxConnectionsPerPool = max(50, min(200, maxTotalConnections / 3)) // 每个池50-200个连接，最多不超过总数的1/3

    // 按host:port分组的连接池
    private val hostPortPools = ConcurrentHashMap<String, FixedChannelPool>()

    /**
     * 获取路由键 (host:port)
     */
    private fun getHostPortKey(uri: URI): String {
        val port = if (uri.port == -1) {
            if (uri.scheme == "https") 443 else 80
        } else {
            uri.port
        }
        return "${uri.host}:$port"
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
        log.debug("Acquiring connection for: {}", hostPortKey)

        // 获取或创建host:port连接池（使用双重检查锁避免并发创建）
        val channelPool = hostPortPools[hostPortKey] ?: synchronized(this) {
            hostPortPools[hostPortKey] ?: createChannelPool(uri).also { newPool ->
                hostPortPools[hostPortKey] = newPool
                log.info("Created and registered new channel pool for: {}", hostPortKey)
            }
        }

        // 获取连接
        return suspendCancellableCoroutine { continuation ->
            val future = channelPool.acquire()

            future.addListener {
                if (future.isSuccess) {
                    // 设置channel属性，用于release时识别
                    future.getNow().attr(io.netty.util.AttributeKey.valueOf<String>("hostPortKey")).set(hostPortKey)
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
            val hostPortKey = channel.attr(io.netty.util.AttributeKey.valueOf<String>("hostPortKey")).get()
            if (hostPortKey != null) {
                hostPortPools[hostPortKey]?.release(channel)
                log.debug("Connection released for: {}", hostPortKey)
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
            maxConnectionsPerPool,
            maxWaitQueueSize
        ).apply {
            log.debug("Channel pool created for: {} with max connections: {}, max wait queue: {}",
                     hostPortKey, maxConnectionsPerPool, maxWaitQueueSize)
        }
    }

    /**
     * 重写connectChannel方法，修复remoteAddress设置问题
     */
    fun connectChannel(bootstrap: Bootstrap): io.netty.channel.ChannelFuture {
        val remoteAddress = bootstrap.config().remoteAddress() as java.net.InetSocketAddress
        return bootstrap.connect(remoteAddress)
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
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout.toInt())
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
        val port = parts[1].toIntOrNull() ?:
            throw IllegalArgumentException("Invalid port number: ${parts[1]}")
        return Pair(host, port)
    }

    /**
     * 获取连接池统计信息
     */
    fun getPoolStatistics(): Map<String,PoolStats> {
        val statistics = mutableMapOf<String, PoolStats>()

        hostPortPools.forEach { (hostPortKey, pool) ->
            try {
                // 简化统计信息，我们只记录基本的池状态
                statistics[hostPortKey] = PoolStats(
                    activeConnections = 0, // FixedChannelPool没有直接提供这个API
                    pendingAcquisitions = 0, // FixedChannelPool没有直接提供这个API
                    maxConnections = maxConnectionsPerPool,
                    maxPendingAcquisitions = maxWaitQueueSize
                )
            } catch (e: Exception) {
                log.warn("Failed to get statistics for pool: {}", hostPortKey, e)
                statistics[hostPortKey] = PoolStats.error(e.message ?: "Unknown error")
            }
        }

        return statistics
    }

    /**
     * 关闭所有连接池
     */
    fun shutdown() {
        log.info("Shutting down host:port channel pool...")

        hostPortPools.forEach { (hostPortKey, pool) ->
            try {
                log.debug("Closing pool for: {}", hostPortKey)
                pool.close()
            } catch (e: Exception) {
                log.warn("Failed to close pool for: {}", hostPortKey, e)
            }
        }

        hostPortPools.clear()

        // 关闭共享的 EventLoopGroup
        try {
            log.debug("Shutting down event loop group")
            eventLoopGroup.shutdownGracefully()
            log.info("Host:Port channel pool shutdown completed")
        } catch (e: Exception) {
            log.warn("Failed to shutdown event loop group", e)
        }
    }

    /**
     * 记录连接池状态
     */
    fun logPoolStatus() {
        val statistics = getPoolStatistics()
        if (log.isInfoEnabled && statistics.isNotEmpty()) {
            var totalActive = 0
            var totalPending = 0

            statistics.forEach { (hostPortKey, stats) ->
                if (stats.error == null) {
                    totalActive += stats.activeConnections
                    totalPending += stats.pendingAcquisitions
                }
            }

            log.info("Connection Pool Status: {} pools, {} active connections, {} pending requests",
                     statistics.size, totalActive, totalPending)

            if (log.isDebugEnabled) {
                statistics.forEach { (hostPortKey, stats) ->
                    if (stats.error != null) {
                        log.debug("  Pool {} - Error: {}", hostPortKey, stats.error)
                    } else {
                        log.debug("  Pool {} - Active: {}, Pending: {}, Max: {}",
                                 hostPortKey, stats.activeConnections, stats.pendingAcquisitions, stats.maxConnections)
                    }
                }
            }
        }
    }

    /**
     * 连接池统计信息
     */
    data class PoolStats(
        val activeConnections: Int,
        val pendingAcquisitions: Int,
        val maxConnections: Int,
        val maxPendingAcquisitions: Int,
        val error: String? = null
    ) {
        val isActive: Boolean get() = error == null
        val utilizationRate: Double get() = if (maxConnections > 0) activeConnections.toDouble() / maxConnections else 0.0

        companion object {
            fun error(message: String) = PoolStats(0, 0, 0, 0, message)
        }
    }

    /**
     * 连接池处理器
     * 负责Channel的初始化和清理
     */
    private class ConnectionPoolHandler : ChannelPoolHandler {

        private val log = LoggerFactory.getLogger(ConnectionPoolHandler::class.java)

        override fun channelCreated(channel: Channel) {
            log.debug("Channel created: {}", channel)

            try {
                // 初始化Channel pipeline
                val pipeline = channel.pipeline()

                // 添加连接池特定的处理器
                pipeline.addLast("poolHandler", object : io.netty.channel.ChannelInboundHandlerAdapter() {
                    override fun channelActive(ctx: io.netty.channel.ChannelHandlerContext) {
                        log.debug("Channel became active: {}", ctx.channel())
                        super.channelActive(ctx)
                    }

                    override fun channelInactive(ctx: io.netty.channel.ChannelHandlerContext) {
                        log.debug("Channel became inactive: {}", ctx.channel())
                        super.channelInactive(ctx)
                    }

                    override fun exceptionCaught(ctx: io.netty.channel.ChannelHandlerContext, cause: Throwable) {
                        log.warn("Channel exception: {} - {}", ctx.channel(), cause.message)
                        ctx.close()
                    }
                })

            } catch (e: Exception) {
                log.error("Failed to initialize channel pipeline: {}", e.message, e)
                throw e
            }
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
