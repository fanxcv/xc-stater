package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import `fun`.fan.xc.plugin.proxy.exception.ConnectionPoolTimeoutException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel as KChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

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
class NettyConnectionPool(private val properties: ProxyProperties) {

    private val log: Logger = LoggerFactory.getLogger(NettyConnectionPool::class.java)

    // 配置参数
    private val maxTotalConnections = properties.pool.maxTotalConnections
    private val connectionIdleTimeout = properties.pool.connectionIdleTimeout
    private val connectionTimeout = properties.pool.connectionTimeout
    private val maxWaitQueueSize = properties.pool.maxWaitQueueSize
    private val healthCheckInterval = properties.pool.healthCheckInterval
    private val connectRetryCount = properties.pool.connectRetryCount

    // 按路由分组的连接池
    private val routePools = ConcurrentHashMap<String, RouteConnectionPool>()

    // 全局连接计数器
    private val totalConnections = AtomicInteger(0)

    // 按路由分组的协程等待通道（无阻塞）
    private val routeWaitChannels = ConcurrentHashMap<String, KChannel<CompletableDeferred<Channel>>>()

    // 协程作用域
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 连接池清理定时器（使用协程）
    private val cleanupJob: Job

    // 健康检查定时器（使用协程）
    private val healthCheckJob: Job

    init {
        // 启动空闲连接清理任务（协程版本）
        cleanupJob = coroutineScope.launch {
            while (isActive) {
                delay(connectionIdleTimeout / 2)
                cleanupIdleConnections()
            }
        }

        // 启动健康检查任务（协程版本）
        healthCheckJob = coroutineScope.launch {
            while (isActive) {
                delay(healthCheckInterval)
                healthCheckConnections()
            }
        }

        log.info(
            "NettyConnectionPool initialized with coroutines: maxTotal={}, idleTimeout={}ms, maxWaitQueueSize={}, healthCheckInterval={}ms, connectRetryCount={}",
            maxTotalConnections,
            connectionIdleTimeout,
            maxWaitQueueSize,
            healthCheckInterval,
            connectRetryCount
        )
    }


    /**
     * 异步获取连接（协程挂起版本）
     * 无阻塞，推荐使用
     *
     * @param uri 目标URI
     * @return 连接Channel
     */
    suspend fun acquireConnectionSuspend(uri: URI): Channel {
        val routeKey = getRouteKey(uri)

        // 获取或创建路由连接池，确保每个路由至少有一个连接配额
        val routePool = routePools.getOrPut(routeKey) {
            val perRouteConnections = max(1, maxTotalConnections - properties.route.size)
            RouteConnectionPool(perRouteConnections)
        }

        // 首先尝试从路由池获取可用连接
        val channel = routePool.acquireConnection()
        if (channel != null) {
            return channel
        }

        // 原子性地检查并尝试创建连接，如果失败则进入等待队列
        return tryCreateConnectionOrWait(routePool, uri)
    }

    /**
     * 原子性地尝试创建连接或进入等待队列
     * 修复竞争条件问题，确保所有请求都能正确处理
     */
    private suspend fun tryCreateConnectionOrWait(routePool: RouteConnectionPool, uri: URI): Channel {
        // 原子性地检查连接池是否有配额并尝试创建连接
        val currentTotal = totalConnections.get()
        if (currentTotal < maxTotalConnections) {
            // 有配额，尝试原子性地增加连接数
            if (totalConnections.compareAndSet(currentTotal, currentTotal + 1)) {
                // 成功获取配额，创建连接
                return try {
                    createNewConnectionSuspend(routePool, uri)
                } catch (e: Exception) {
                    // 创建连接失败，释放配额
                    totalConnections.decrementAndGet()
                    throw e
                }
            }
        }

        // 没有配额或CAS失败，进入等待队列
        return waitForConnectionSuspend(uri)
    }

    /**
     * 等待获取连接（按路由分组的等待队列）
     */
    private suspend fun waitForConnectionSuspend(uri: URI): Channel {
        val routeKey = getRouteKey(uri)

        // 获取或创建路由等待通道
        val waitChannel = routeWaitChannels.getOrPut(routeKey) {
            KChannel(maxWaitQueueSize)
        }

        val deferred = CompletableDeferred<Channel>()

        // 尝试发送到等待通道
        val sendResult = waitChannel.trySend(deferred)
        if (sendResult.isFailure) {
            log.warn("Wait queue exhausted for route: {}", routeKey)
            throw ConnectionPoolTimeoutException("Wait queue exhausted for route $routeKey")
        }

        log.debug("Request entered wait queue for route: {}", routeKey)

        // 等待连接可用（带超时）
        return withTimeout(connectionTimeout) {
            deferred.await()
        }
    }


    /**
     * 通知等待队列有连接可用
     */
    private fun notifyWaitQueue(routeKey: String) {
        val waitChannel = routeWaitChannels[routeKey] ?: return

        coroutineScope.launch {
            val deferred = waitChannel.tryReceive().getOrNull() ?: return@launch

            try {
                val routePool = routePools.getOrPut(routeKey) {
                    val perRouteConnections = max(1, maxTotalConnections / max(1, properties.route.size))
                    RouteConnectionPool(perRouteConnections)
                }

                // 首先尝试从路由池获取可用连接
                val existingChannel = routePool.acquireConnection()
                if (existingChannel != null) {
                    deferred.complete(existingChannel)
                } else {
                    // 没有空闲连接，按新的原子性逻辑为等待队列创建连接
                    processWaitQueueWithConnectionCreation(routePool, routeKey, deferred, waitChannel)
                }
            } catch (e: Exception) {
                log.error("Error in notifyWaitQueue for route {}: {}", routeKey, e.message, e)
                deferred.completeExceptionally(e)
            }
        }
    }

    /**
     * 为等待队列处理带连接创建的请求
     */
    private suspend fun processWaitQueueWithConnectionCreation(
        routePool: RouteConnectionPool,
        routeKey: String,
        deferred: CompletableDeferred<Channel>,
        waitChannel: KChannel<CompletableDeferred<Channel>>
    ) {
        // 原子性地尝试为等待队列请求创建连接
        val currentTotal = totalConnections.get()
        if (currentTotal < maxTotalConnections) {
            // 有配额，尝试原子性地增加连接数
            if (totalConnections.compareAndSet(currentTotal, currentTotal + 1)) {
                // 成功获取配额，为等待队列创建连接
                try {
                    val uri = parseRouteKeyToUri(routeKey)
                    val newChannel = createNewConnectionSuspend(routePool, uri)
                    deferred.complete(newChannel)
                    return
                } catch (e: Exception) {
                    // 创建连接失败，释放配额
                    totalConnections.decrementAndGet()
                    log.warn("Failed to create connection for wait queue: {}", routeKey)
                    deferred.completeExceptionally(e)
                    return
                }
            }
        }

        // 没有配额或CAS失败，重新放回队列等待下次机会
        val sendResult = waitChannel.trySend(deferred)
        if (sendResult.isFailure) {
            log.warn("Wait channel full for route: {}", routeKey)
            deferred.completeExceptionally(ConnectionPoolTimeoutException("No available connection and wait queue full"))
        }
    }

    /**
     * 释放连接回连接池
     *
     * @param channel 连接Channel
     */
    fun releaseConnection(channel: Channel) {
        if (!channel.isActive) {
            closeChannel(channel)
            // 连接关闭时也要通知等待队列，可能可以创建新连接
            val routeKey = channel.attr(ROUTE_KEY_ATTR).get()
            if (routeKey != null) {
                notifyWaitQueue(routeKey)
            }
            return
        }

        val routeKey = channel.attr(ROUTE_KEY_ATTR).get()
        if (routeKey == null) {
            closeChannel(channel)
            return
        }

        val routePool = routePools[routeKey]
        if (routePool != null) {
            routePool.releaseConnection(channel)
            log.debug("Connection released for route: {}", routeKey)

            // 通知等待队列有连接可用
            notifyWaitQueue(routeKey)
        } else {
            closeChannel(channel)
        }
    }

    /**
     * 创建新连接（连接数已在调用方检查）
     */
    private suspend fun createNewConnectionSuspend(routePool: RouteConnectionPool, uri: URI): Channel {
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
            val channel = withTimeout(connectionTimeout) {
                suspendCancellableCoroutine<Channel> { continuation ->
                    val connectFuture = bootstrap.connect(uri.host, port)

                    continuation.invokeOnCancellation {
                        connectFuture.cancel(true)
                    }

                    connectFuture.addListener { future ->
                        if (future.isSuccess) {
                            continuation.resume(connectFuture.channel())
                        } else {
                            continuation.resumeWithException(
                                future.cause() ?: TimeoutException("Failed to connect to ${uri.host}:${port}")
                            )
                        }
                    }
                }
            }

            // 设置路由键属性
            channel.attr(ROUTE_KEY_ATTR).set(getRouteKey(uri))

            // 创建连接信息并添加到路由池
            val connectionInfo = ConnectionInfo(channel, System.currentTimeMillis())
            routePool.addConnection(connectionInfo)

            log.debug("Created new connection for route: {}", getRouteKey(uri))

            return channel
        } catch (e: Exception) {
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
        }
        val previousCount = totalConnections.getAndDecrement()
        if (previousCount > 0) {
            log.debug(
                "Closed connection: {}, total connections: {}/{}",
                channel,
                totalConnections.get(),
                maxTotalConnections
            )

            // 连接关闭后，通知所有等待队列可能有空间创建新连接
            routeWaitChannels.keys.forEach { routeKey ->
                notifyWaitQueue(routeKey)
            }
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
     * 从路由键解析回 URI
     */
    private fun parseRouteKeyToUri(routeKey: String): URI {
        return URI.create(routeKey)
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
     * 处理等待队列
     */

    /**
     * 健康检查连接
     */
    private fun healthCheckConnections() {
        var totalChecked = 0
        var totalFailed = 0

        routePools.values.forEach { routePool ->
            val (checked, failed) = routePool.healthCheckConnections()
            totalChecked += checked
            totalFailed += failed
        }

        if (totalFailed > 0) {
            log.info(
                "Health check completed: checked={}, failed={}, total connections={}/{}",
                totalChecked, totalFailed, totalConnections.get(), maxTotalConnections
            )
        } else if (totalChecked > 0) {
            log.debug("Health check completed: all {} connections are healthy", totalChecked)
        }
    }

    /**
     * 关闭连接池
     */
    fun shutdown() {
        log.info("Shutting down connection pool...")

        // 取消所有协程任务
        cleanupJob.cancel()
        healthCheckJob.cancel()
        coroutineScope.cancel()

        // 清空所有路由等待通道并拒绝所有等待请求
        routeWaitChannels.values.forEach { waitChannel ->
            while (true) {
                val deferred = waitChannel.tryReceive().getOrNull() ?: break
                deferred.completeExceptionally(TimeoutException("Connection pool shutdown"))
            }
            waitChannel.close()
        }
        routeWaitChannels.clear()

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
    private inner class RouteConnectionPool(@Suppress("UNUSED_PARAMETER") maxConnections: Int = maxTotalConnections) {

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
         * 健康检查连接
         */
        fun healthCheckConnections(): Pair<Int, Int> {
            var checked = 0
            var failed = 0

            synchronized(connectionLock) {
                // 检查空闲连接
                val idleIterator = idleConnections.iterator()
                while (idleIterator.hasNext()) {
                    val connectionInfo = idleIterator.next()
                    val channel = connectionInfo.channel

                    if (!channel.isActive) {
                        // 连接已关闭
                        idleIterator.remove()
                        totalConnections.decrementAndGet()
                        failed++
                    } else {
                        // 简单健康检查：尝试写入一个空操作
                        try {
                            if (channel.isWritable) {
                                // 连接健康
                                checked++
                            } else {
                                // 连接不可写，可能有问题
                                idleIterator.remove()
                                closeChannel(channel)
                                failed++
                            }
                        } catch (_: Exception) {
                            // 健康检查失败
                            idleIterator.remove()
                            closeChannel(channel)
                            failed++
                        }
                    }
                }

                // 检查活跃连接（虽然通常在释放时会检查）
                val activeIterator = activeConnections.entries.iterator()
                while (activeIterator.hasNext()) {
                    val (channel, _) = activeIterator.next()
                    if (!channel.isActive) {
                        activeIterator.remove()
                        totalConnections.decrementAndGet()
                        failed++
                    }
                }
            }

            return Pair(checked, failed)
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
