package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.pool.ChannelPoolHandler
import io.netty.channel.pool.FixedChannelPool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * 基于host:port的Netty连接池
 * 管理HTTP/HTTPS连接的复用和池化，按目标host:port分组管理连接
 *
 * @author fan
 *
 * ## 功能特性
 * - 支持HTTP/HTTPS连接池化管理
 * - 按目标host:port分组管理连接
 * - 支持连接复用，减少连接创建开销
 * - 自动清理空闲连接
 * - 可配置连接池参数
 * - 限制每个子连接池的最大连接数，避免某一个高并发连接将所有pool都耗尽
 *
 * ## 配置参数
 * - maxTotalConnections: 最大总连接数
 * - connectionIdleTimeout: 连接空闲超时时间
 * - connectionTimeout: 连接超时时间
 * - maxWaitQueueSize: 最大等待队列长度
 * - healthCheckInterval: 健康检查间隔
 * - connectRetryCount: 连接创建失败重试次数
 */
class HostPortChannelPool(private val properties: ProxyProperties) {
    private val log: Logger = LoggerFactory.getLogger(HostPortChannelPool::class.java)

    // 配置参数
    private val maxTotalConnections = properties.pool.maxTotalConnections
    private val connectionIdleTimeout = properties.pool.connectionIdleTimeout
    private val connectionTimeout = properties.pool.connectionTimeout
    private val maxWaitQueueSize = properties.pool.maxWaitQueueSize
    private val healthCheckInterval = properties.pool.healthCheckInterval
    private val connectRetryCount = properties.pool.connectRetryCount

    // 按host:port分组的连接池
    private val hostPortPools = ConcurrentHashMap<String, FixedChannelPool>()

    // 连接池大小限制的锁
    private val poolSizeMutex = Mutex()

    // 全局连接计数器
    private val totalConnections = java.util.concurrent.atomic.AtomicInteger(0)

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
     * 无阻塞，推荐使用
     *
     * @param uri 目标URI
     * @return 连接Channel
     */
    suspend fun acquireConnectionSuspend(uri: URI): Channel {
        val hostPortKey = getHostPortKey(uri)

        // 获取或创建host:port连接池
        val channelPool = hostPortPools.getOrPut(hostPortKey) {
            createChannelPool(uri)
        }

        // 动态调整连接池大小
        adjustPoolSizes()

        // 获取连接
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val future = channelPool.acquire()
            future.addListener {
                if (future.isSuccess) {
                    // 设置channel属性，用于release时识别
                    future.getNow().attr(io.netty.util.AttributeKey.valueOf<String>("hostPortKey")).set(hostPortKey)
                    continuation.resume(future.getNow()) {
                        // onCancellation 回调
                        if (future.isSuccess && future.isDone) {
                            try {
                                channelPool.release(future.getNow())
                            } catch (e: Exception) {
                                log.warn("Failed to release connection on cancellation: ${e.message}")
                            }
                        }
                    }
                } else {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            future.cause() ?: RuntimeException("Failed to acquire connection")
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                // 如果协程被取消，确保释放连接
                if (future.isSuccess && future.isDone) {
                    try {
                        channelPool.release(future.getNow())
                    } catch (e: Exception) {
                        log.warn("Failed to release connection on cancellation: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * 创建ChannelPool
     */
    private fun createChannelPool(uri: URI): FixedChannelPool {
        val clientFactory = NettyClientFactory.getInstance()

        // 根据协议设置初始化器
        val port = if (uri.port == -1) {
            if (uri.scheme == "https") 443 else 80
        } else {
            uri.port
        }

        // 创建原始TCP连接初始化器，不包含HTTP编解码器
        val initializer = object : io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
            override fun initChannel(ch: io.netty.channel.socket.SocketChannel) {
                val pipeline = ch.pipeline()

                if (uri.scheme == "https") {
                    // 添加SSL处理器用于HTTPS
                    val sslContext = clientFactory.getSslContext(true)
                    pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()))
                }

                // 添加超时处理器，但不在连接池中添加HTTP编解码器
                // HTTP编解码器将在ProxyClient中动态添加
                pipeline.addLast("idle", io.netty.handler.timeout.IdleStateHandler(
                    connectionTimeout * 2, 0, 0, java.util.concurrent.TimeUnit.MILLISECONDS
                ))
            }
        }

        // 创建带有远程地址解析器的Bootstrap
        val bootstrap = Bootstrap()
            .group(clientFactory.getEventLoopGroup())
            .channel(clientFactory.getSocketChannelClass())
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout.toInt())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .resolver(io.netty.resolver.DefaultAddressResolverGroup.INSTANCE) // 添加DNS解析器
            .handler(initializer)

        // 计算每个子连接池的最大连接数
        val maxConnectionsPerPool = calculateMaxConnectionsPerPool()

        // 创建自定义的FixedChannelPool，在连接时指定远程地址
        return object : FixedChannelPool(bootstrap, createChannelPoolHandler(uri), maxConnectionsPerPool) {
            override fun connectChannel(bootstrap: io.netty.bootstrap.Bootstrap): io.netty.channel.ChannelFuture {
                // 在连接时明确指定远程地址
                val remoteAddress = java.net.InetSocketAddress(uri.host, port)
                log.debug("Connecting to remote address: {}", remoteAddress)
                return bootstrap.connect(remoteAddress)
            }
        }
    }

    /**
     * 创建ChannelPool处理器
     */
    private fun createChannelPoolHandler(uri: URI): ChannelPoolHandler {
        return object : ChannelPoolHandler {
            override fun channelCreated(ch: Channel) {
                // 增加全局连接计数
                totalConnections.incrementAndGet()
                // 避免在连接未完全建立时访问remoteAddress导致的错误
                val addrInfo = try {
                    ch.remoteAddress()?.toString() ?: "not connected"
                } catch (e: Exception) {
                    "address unavailable"
                }
                log.debug("Channel created for host:port {}: {}", getHostPortKey(uri), addrInfo)
            }

            override fun channelAcquired(ch: Channel) {
                val addrInfo = try {
                    ch.remoteAddress()?.toString() ?: "not connected"
                } catch (e: Exception) {
                    "address unavailable"
                }
                log.debug("Channel acquired for host:port {}: {}", getHostPortKey(uri), addrInfo)
            }

            override fun channelReleased(ch: Channel) {
                val addrInfo = try {
                    ch.remoteAddress()?.toString() ?: "not connected"
                } catch (e: Exception) {
                    "address unavailable"
                }
                log.debug("Channel released for host:port {}: {}", getHostPortKey(uri), addrInfo)
            }
        }
    }

    /**
     * 计算每个子连接池的最大连接数
     * 限制每个子pool的大小为1到最大连接数-路由总数，避免某一个高并发连接将所有pool都耗尽
     */
    private fun calculateMaxConnectionsPerPool(): Int {
        val routeCount = properties.route?.size ?: 0
        val hostPortCount = hostPortPools.size.coerceAtLeast(1)
        return max(1, (maxTotalConnections - routeCount) / hostPortCount)
    }

    /**
     * 动态调整连接池大小
     */
    private suspend fun adjustPoolSizes() {
        poolSizeMutex.withLock {
            val maxConnectionsPerPool = calculateMaxConnectionsPerPool()

            // 更新所有现有连接池的大小
            hostPortPools.values.forEach { pool ->
                // 注意：FixedChannelPool的大小在创建时就固定了，无法动态调整
                // 这里只是记录日志，实际的大小调整需要重新创建连接池
                log.debug("Calculated max connections per pool: {}", maxConnectionsPerPool)
            }
        }
    }

    /**
     * 释放连接回连接池
     *
     * @param channel 连接Channel
     */
    fun releaseConnection(channel: Channel) {
        // 从channel属性中获取host:port键
        val hostPortKey = channel.attr(io.netty.util.AttributeKey.valueOf<String>("hostPortKey")).get()
        if (hostPortKey != null) {
            val channelPool = hostPortPools[hostPortKey]
            if (channelPool != null) {
                try {
                    channelPool.release(channel)
                    log.debug("Connection released for host:port: {}", hostPortKey)
                } catch (e: Exception) {
                    log.warn("Failed to release connection for host:port {}: {}", hostPortKey, e.message)
                    // 如果释放失败，关闭连接并减少计数器
                    closeChannel(channel)
                }
            } else {
                // 如果找不到对应的连接池，直接关闭连接
                closeChannel(channel)
            }
        } else {
            // 如果没有host:port键，直接关闭连接
            closeChannel(channel)
        }
    }

    /**
     * 关闭连接
     */
    private fun closeChannel(channel: Channel) {
        if (channel.isActive) {
            channel.close()
        }
        // 减少全局连接计数
        val previousCount = totalConnections.getAndDecrement()
        if (previousCount > 0) {
            log.debug(
                "Closed connection: {}, total connections: {}/{}",
                channel,
                totalConnections.get(),
                maxTotalConnections
            )
        }
    }

    /**
     * 关闭连接池
     */
    fun shutdown() {
        log.info("Shutting down host:port connection pool...")

        // 关闭所有连接池
        hostPortPools.values.forEach { pool ->
            try {
                pool.close()
            } catch (e: Exception) {
                log.warn("Error closing channel pool: ${e.message}")
            }
        }

        hostPortPools.clear()
        log.info("Host:port connection pool shutdown completed")
    }
}
