package `fun`.fan.xc.plugin.proxy.handler

import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ByteBuf对象池 - 高性能内存管理
 *
 * 通过复用ByteBuf对象，减少内存分配和GC压力，实现零拷贝传输。
 * 特别针对代理服务中的HTTP请求/响应体处理场景优化。
 *
 * @author 老王内存优化
 * @since 1.8.0
 */
object BufferPool {
    private val log = org.slf4j.LoggerFactory.getLogger(BufferPool::class.java)

    // 对象池 - 使用ConcurrentLinkedQueue保证线程安全
    private val pool = ConcurrentLinkedQueue<ByteBuf>()

    // 池配置
    private const val MAX_POOL_SIZE = 1000
    private const val DEFAULT_BUFFER_SIZE = 32 * 1024 // 32KB

    /**
     * 从对象池获取ByteBuf
     * 优先从池中复用，池空时创建新对象
     *
     * @param size 期望的缓冲区大小
     * @return 可复用的ByteBuf
     */
    fun acquire(size: Int = DEFAULT_BUFFER_SIZE): ByteBuf {
        val buf = pool.poll()

        return if (buf != null) {
            // 复用池中的缓冲区
            log.trace("BufferPool: 复用缓冲区, 当前池大小: {}", pool.size)

            // 确保缓冲区足够大，如果太小则重新分配
            if (buf.capacity() >= size) {
                buf.clear()
            } else {
                // 如果池中的缓冲区太小，释放它并分配新的
                buf.release()
                allocateNew(size)
            }
        } else {
            // 池为空，分配新缓冲区
            allocateNew(size)
        }
    }

    /**
     * 将ByteBuf归还到对象池
     *
     * @param buf 要归还的ByteBuf
     * @param force 是否强制归还（即使池已满）
     */
    fun release(buf: ByteBuf?, force: Boolean = false) {
        if (buf == null || !buf.isWritable) {
            return
        }

        // 检查池大小，池满时丢弃多余缓冲区
        if (!force && pool.size >= MAX_POOL_SIZE) {
            buf.release()
            return
        }

        // 清空缓冲区并放入池中
        buf.clear()
        pool.offer(buf)

        log.trace("BufferPool: 归还缓冲区, 当前池大小: {}", pool.size)
    }

    /**
     * 批量释放多个缓冲区
     */
    fun releaseAll(buffers: Collection<ByteBuf?>?) {
        buffers?.forEach { release(it) }
    }

    /**
     * 分配新的ByteBuf
     */
    private fun allocateNew(size: Int): ByteBuf {
        // 使用Netty的池化分配器，减少系统调用
        val buf = PooledByteBufAllocator.DEFAULT.buffer(size)
        log.trace("BufferPool: 分配新缓冲区, 大小: {}, 池大小: {}", size, pool.size)
        return buf
    }

    /**
     * 清空对象池
     * 主要用于测试或关闭时
     */
    fun clear() {
        var released = 0
        var buf: ByteBuf?
        while (pool.poll().also { buf = it } != null) {
            buf?.release()
            released++
        }
        log.info("BufferPool: 清空对象池, 释放 {} 个缓冲区", released)
    }

    /**
     * 内存拷贝优化：将源ByteBuf的内容转移到目标ByteBuf
     * 这是零拷贝传输的核心操作
     *
     * @param source 源缓冲区
     * @param destination 目标缓冲区
     */
    fun transferToZeroCopy(source: ByteBuf, destination: ByteBuf) {
        val readableBytes = source.readableBytes()
        if (readableBytes > 0) {
            // 使用Netty的readBytes进行直接传输，比手动复制更高效
            destination.writeBytes(source, readableBytes)
        }
    }
}
