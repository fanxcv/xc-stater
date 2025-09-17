package `fun`.fan.xc.plugin.proxy.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Netty连接池
 * 管理HTTP/HTTPS连接的复用和池化
 *
 * @author fan
 *
 * ## 功能特性
 * - 支持HTTP/HTTPS连接池化管理
 * - 按路由分组管理连接
 * - 支持连接复用，减少连接创建开销
 * - 自动清理空闲连接
 * - 可配置连接池参数
 *
 * ## 配置参数
 * - maxTotalConnections: 最大总连接数
 * - maxConnectionsPerRoute: 每路由最大连接数
 * - connectionIdleTimeout: 连接空闲超时时间
 * - connectionTimeout: 连接超时时间
 * - maxWaitQueueSize: 最大等待队列长度
 * - healthCheckInterval: 健康检查间隔
 * - connectRetryCount: 连接创建失败重试次数
 */
class NettyConnectionPool(
    private val maxTotalConnections: Int = 200,
    private val maxConnectionsPerRoute: Int = 20,
    private val connectionIdleTimeout: Long = 300000L, // 5分钟
    private val connectionTimeout: Long = 5000L, // 5秒
    private val maxWaitQueueSize: Int = 100, // 最大等待队列长度
    private val healthCheckInterval: Long = 60000L, // 连接健康检查间隔 1分钟
    private val connectRetryCount: Int = 3 // 连接创建失败重试次数
) {

    private val log: Logger = LoggerFactory.getLogger(NettyConnectionPool::class.java)

    // 按路由分组的连接池
    private val routePools = ConcurrentHashMap<String, RouteConnectionPool>()

    // 全局连接计数器
    private val totalConnections = AtomicInteger(0)

    // 连接池清理定时器
    private val cleanupTimer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r, "NettyConnectionPool-Cleanup")
        thread.isDaemon = true
        thread
    }

    init {
        // 启动空闲连接清理任务
        cleanupTimer.scheduleAtFixedRate(
            this::cleanupIdleConnections,
            connectionIdleTimeout / 2,
            connectionIdleTimeout / 2,
            TimeUnit.MILLISECONDS
        )

        log.info(
            "NettyConnectionPool initialized: maxTotal={}, maxPerRoute={}, idleTimeout={}ms, maxWaitQueueSize={}, healthCheckInterval={}ms, connectRetryCount={}",
            maxTotalConnections, maxConnectionsPerRoute, connectionIdleTimeout, maxWaitQueueSize, healthCheckInterval, connectRetryCount
        )
    }

    /**
     * 获取连接
     *
     * @param uri 目标URI
     * @return 连接Channel
     */
    @Throws(TimeoutException::class, InterruptedException::class)
    fun acquireConnection(uri: URI): Channel {
        val routeKey = getRouteKey(uri)

        // 获取或创建路由连接池
        val routePool = routePools.getOrPut(routeKey) {
            RouteConnectionPool()
        }

        // 尝试从路由池获取可用连接
        val channel = routePool.acquireConnection() ?: createNewConnection(routePool, uri)

        log.debug(
            "Acquired connection for route: {}, active connections: {}/{}",
            routeKey, totalConnections.get(), maxTotalConnections
        )
        log.info("Connection pool status - route: {}, active: {}, idle: {}, total: {}/{}",
            routeKey, routePool.getActiveConnectionCount(), routePool.getIdleConnectionCount(),
            totalConnections.get(), maxTotalConnections)

        return channel
    }

    /**
     * 释放连接回连接池
     *
     * @param channel 连接Channel
     */
    fun releaseConnection(channel: Channel) {
        if (!channel.isActive) {
            log.debug("Channel is not active, closing: {}", channel)
            closeChannel(channel)
            return
        }

        val routeKey = channel.attr(ROUTE_KEY_ATTR).get()
        if (routeKey == null) {
            log.warn("Channel has no route key attribute, closing: {}", channel)
            closeChannel(channel)
            return
        }

        val routePool = routePools[routeKey]
        if (routePool != null) {
            routePool.releaseConnection(channel)
            log.debug("Released connection for route: {}", routeKey)
        } else {
            log.warn("No route pool found for route: {}, closing channel: {}", routeKey, channel)
            closeChannel(channel)
        }
    }

    /**
     * 创建新连接
     */
    @Throws(TimeoutException::class, InterruptedException::class)
    private fun createNewConnection(routePool: RouteConnectionPool, uri: URI): Channel {
        // 检查全局连接数限制
        while (true) {
            val currentTotal = totalConnections.get()
            if (currentTotal >= maxTotalConnections) {
                throw TimeoutException("Connection pool exhausted: max total connections reached")
            }
            if (totalConnections.compareAndSet(currentTotal, currentTotal + 1)) {
                break
            }
        }

        try {
            val clientFactory = NettyClientFactory.getInstance()
            val bootstrap = Bootstrap()
                .group(clientFactory.getEventLoopGroup())
                .channel(clientFactory.getSocketChannelClass())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout.toInt())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)

            // 根据协议设置初始化器
            val port = if (uri.port == -1) {
                if (uri.scheme == "https") 443 else 80
            } else {
                uri.port
            }

            val initializer = if (uri.scheme == "https") {
                clientFactory.createHttpsClientInitializer(
                    trustAll = true, // 生产环境应该谨慎使用
                    maxContentLength = 65536,
                    timeoutMs = connectionTimeout
                )
            } else {
                clientFactory.createHttpClientInitializer(
                    maxContentLength = 65536,
                    timeoutMs = connectionTimeout
                )
            }

            bootstrap.handler(initializer)

            // 连接超时控制
            val connectFuture = bootstrap.connect(uri.host, port)
            val completed = connectFuture.await(connectionTimeout, TimeUnit.MILLISECONDS)

            if (!completed) {
                connectFuture.cancel(true)
                throw TimeoutException("Connection timeout to ${uri.host}:${port}")
            }

            if (!connectFuture.isSuccess) {
                throw connectFuture.cause() ?: TimeoutException("Failed to connect to ${uri.host}:${port}")
            }

            val channel = connectFuture.channel()

            // 设置路由键属性
            val routeKey = getRouteKey(uri)
            channel.attr(ROUTE_KEY_ATTR).set(routeKey)

            // 创建连接信息并添加到路由池
            val connectionInfo = ConnectionInfo(channel, System.currentTimeMillis())
            routePool.addConnection(connectionInfo)

            log.info(
                "Created new connection for route: {}, total connections: {}/{}",
                routeKey, totalConnections.get(), maxTotalConnections
            )

            return channel
        } catch (e: Exception) {
            totalConnections.decrementAndGet()
            log.error("Failed to create connection for route {}: {}", getRouteKey(uri), e.message)
            throw e
        }
    }

    /**
     * 关闭连接
     */
    private fun closeChannel(channel: Channel) {
        if (channel.isActive) {
            channel.close()
            totalConnections.decrementAndGet()
            log.debug("Closed connection: {}", channel)
        }
    }

    /**
     * 获取路由键
     */
    private fun getRouteKey(uri: URI): String {
        val port = if (uri.port == -1) {
            if (uri.scheme == "https") 443 else 80
        } else {
            uri.port
        }
        return "${uri.scheme}://${uri.host}:$port"
    }

    /**
     * 清理空闲连接
     */
    private fun cleanupIdleConnections() {
        val currentTime = System.currentTimeMillis()
        var totalCleaned = 0

        routePools.values.forEach { routePool ->
            val cleaned = routePool.cleanupIdleConnections(currentTime, connectionIdleTimeout)
            totalCleaned += cleaned
        }

        if (totalCleaned > 0) {
            log.info(
                "Cleaned up {} idle connections, total active connections: {}/{}",
                totalCleaned, totalConnections.get(), maxTotalConnections
            )
        }
    }

    /**
     * 关闭连接池
     */
    fun shutdown() {
        log.info("Shutting down connection pool...")

        // 取消清理任务
        cleanupTimer.shutdown()

        // 关闭所有连接
        routePools.values.forEach { routePool ->
            routePool.shutdown()
        }

        routePools.clear()
        log.info("Connection pool shutdown completed")
    }

    /**
     * 路由连接池
     */
    private inner class RouteConnectionPool() {

        /**
         * 获取活跃连接数
         */
        fun getActiveConnectionCount(): Int {
            return activeConnections.size
        }

        /**
         * 获取空闲连接数
         */
        fun getIdleConnectionCount(): Int {
            return idleConnections.size
        }
        private val activeConnections = ConcurrentHashMap<Channel, ConnectionInfo>()
        private val idleConnections = ConcurrentLinkedDeque<ConnectionInfo>()
        private val connectionLock = Any()

        /**
         * 获取可用连接
         */
        fun acquireConnection(): Channel? {
            synchronized(connectionLock) {
                while (true) {
                    val connectionInfo = idleConnections.poll() ?: break

                    val channel = connectionInfo.channel
                    if (channel.isActive) {
                        // 将连接从空闲状态移到活跃状态
                        activeConnections[channel] = connectionInfo
                        return channel
                    } else {
                        // 连接已关闭，清理
                        totalConnections.decrementAndGet()
                    }
                }
                return null
            }
        }

        /**
         * 添加新连接
         */
        fun addConnection(connectionInfo: ConnectionInfo) {
            synchronized(connectionLock) {
                activeConnections[connectionInfo.channel] = connectionInfo
            }
        }

        /**
         * 释放连接
         */
        fun releaseConnection(channel: Channel) {
            val connectionInfo = activeConnections[channel]
            if (connectionInfo == null) {
                log.warn("Connection not found in active pool for channel: {}", channel)
                return
            }

            synchronized(connectionLock) {
                if (activeConnections.remove(channel) != null) {
                    connectionInfo.lastUsedTime = System.currentTimeMillis()
                    idleConnections.offer(connectionInfo)
                }
            }
        }

        /**
         * 清理空闲连接
         */
        fun cleanupIdleConnections(currentTime: Long, idleTimeout: Long): Int {
            var cleaned = 0

            synchronized(connectionLock) {
                val iterator = idleConnections.iterator()
                while (iterator.hasNext()) {
                    val connectionInfo = iterator.next()
                    if (currentTime - connectionInfo.lastUsedTime > idleTimeout) {
                        iterator.remove()
                        closeChannel(connectionInfo.channel)
                        cleaned++
                    }
                }
            }

            return cleaned
        }

        /**
         * 关闭路由池
         */
        fun shutdown() {
            synchronized(connectionLock) {
                activeConnections.keys.forEach { channel ->
                    closeChannel(channel)
                }
                idleConnections.forEach { connectionInfo ->
                    closeChannel(connectionInfo.channel)
                }
                activeConnections.clear()
                idleConnections.clear()
            }
        }
    }

    /**
     * 连接信息
     */
    private data class ConnectionInfo(
        val channel: Channel,
        var lastUsedTime: Long
    )

    companion object {
        // 连接路由键属性
        private val ROUTE_KEY_ATTR = io.netty.util.AttributeKey.valueOf<String>("routeKey")
    }
}
