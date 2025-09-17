package `fun`.fan.xc.plugin.proxy.client

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.IdleStateHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Netty客户端工厂
 * 负责创建和管理Netty客户端，复用EventLoopGroup
 *
 * @author fan
 *
 * ## 功能特性
 * - 单例模式管理Netty客户端组件
 * - 复用EventLoopGroup，减少资源消耗
 * - 支持HTTP/HTTPS协议
 * - SSL上下文缓存，提高性能
 * - 自动资源释放，确保JVM关闭时正确清理
 *
 * ## 使用说明
 * 通过getInstance()方法获取单例实例，确保全局唯一性。
 */
class NettyClientFactory {

    private val log: Logger = LoggerFactory.getLogger(NettyClientFactory::class.java)

    companion object {
        @Volatile
        private var instance: NettyClientFactory? = null

        fun getInstance(): NettyClientFactory {
            return instance ?: synchronized(this) {
                instance ?: NettyClientFactory().also { instance = it }
            }
        }
    }

    // 全局复用的EventLoopGroup
    private val workerGroup: EventLoopGroup = NioEventLoopGroup(
        Runtime.getRuntime().availableProcessors() * 2,
        Executors.defaultThreadFactory()
    )

    // SSL上下文缓存
    private val sslContextMap = mutableMapOf<Boolean, SslContext>()

    init {
        log.info(
            "NettyClientFactory initialized with {} worker threads",
            Runtime.getRuntime().availableProcessors() * 2
        )

        // 添加JVM关闭钩子，确保资源正确释放
        Runtime.getRuntime().addShutdownHook(Thread {
            log.info("Shutting down NettyClientFactory...")
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS)
                .sync()
            log.info("NettyClientFactory shutdown completed")
        })
    }

    /**
     * 获取EventLoopGroup
     */
    fun getEventLoopGroup(): EventLoopGroup {
        return workerGroup
    }

    /**
     * 获取SSL上下文
     *
     * @param trustAll 是否信任所有证书
     * @return SSL上下文
     */
    @Throws(SSLException::class)
    fun getSslContext(trustAll: Boolean = false): SslContext {
        return sslContextMap.getOrPut(trustAll) {
            if (trustAll) {
                SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
            } else {
                SslContextBuilder.forClient().build()
            }
        }
    }

    /**
     * 获取SocketChannel类
     */
    fun getSocketChannelClass(): Class<out NioSocketChannel> {
        return NioSocketChannel::class.java
    }

    /**
     * 创建标准的HTTP客户端初始化器
     *
     * @param maxContentLength 最大内容长度
     * @param timeoutMs 超时时间(毫秒)
     * @return 初始化器函数
     */
    fun createHttpClientInitializer(
        maxContentLength: Int = 65536,
        timeoutMs: Long = 5000
    ): io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel> {
        return object : io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
            override fun initChannel(ch: io.netty.channel.socket.SocketChannel) {
                val pipeline = ch.pipeline()

                // 添加超时处理器
                // 只在读取状态下使用超时，写入和全部状态不设置超时
                // 设置为请求超时时间的2倍，确保有足够的时间处理响应
                pipeline.addLast("idle", IdleStateHandler(timeoutMs * 2, 0, 0, TimeUnit.MILLISECONDS))

                // 添加HTTP编解码器
                pipeline.addLast("codec", HttpClientCodec())

                // 添加HTTP聚合器
                pipeline.addLast("aggregator", HttpObjectAggregator(maxContentLength))

                log.debug("HTTP pipeline initialized for channel: {}", ch)
            }
        }
    }

    /**
     * 创建HTTPS客户端初始化器
     *
     * @param trustAll 是否信任所有证书
     * @param maxContentLength 最大内容长度
     * @param timeoutMs 超时时间(毫秒)
     * @return 初始化器函数
     */
    @Throws(SSLException::class)
    fun createHttpsClientInitializer(
        trustAll: Boolean = false,
        maxContentLength: Int = 65536,
        timeoutMs: Long = 5000
    ): io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel> {
        val sslContext = getSslContext(trustAll)

        return object : io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
            override fun initChannel(ch: io.netty.channel.socket.SocketChannel) {
                val pipeline = ch.pipeline()

                // 添加SSL处理器
                pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()))

                // 添加超时处理器
                // 只在读取状态下使用超时，写入和全部状态不设置超时
                // 设置为请求超时时间的2倍，确保有足够的时间处理响应
                pipeline.addLast("idle", IdleStateHandler(timeoutMs * 2, 0, 0, TimeUnit.MILLISECONDS))

                // 添加HTTP编解码器
                pipeline.addLast("codec", HttpClientCodec())

                // 添加HTTP聚合器
                pipeline.addLast("aggregator", HttpObjectAggregator(maxContentLength))

                log.debug("HTTPS pipeline initialized for channel: {}", ch)
            }
        }
    }
}
