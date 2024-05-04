package `fun`.fan.xc.plugin.token_manager

import `fun`.fan.xc.starter.exception.XcServiceException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

/**
 * 基于AtomicInteger实现的TokenManager, 仅适用于单机部署使用
 */
abstract class DefaultTokenManager : BaseTokenManager() {
    protected class TokenEntity : TokenManager.BaseTokenEntity() {
        /**
         * Token更新次数
         */
        val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)
    }

    /**
     * 用于实现自旋锁
     */
    private val lock: ReentrantLock = ReentrantLock()

    /**
     * 用于记录Token更新次数
     */
    private val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)
    protected val entity = TokenEntity()

    override fun token(): String {
        this.checkTokenUpdate()
        return entity.token ?: throw XcServiceException("${key()}: get token failed")
    }

    override fun expires(): Long {
        this.checkTokenUpdate()
        return entity.expires
    }

    override fun init() {
        this.entity.updateCount.set(Int.MIN_VALUE)
        this.updateCount.set(Int.MIN_VALUE)
        this.checkTokenUpdate()
    }

    override fun refresh() {
        this.checkTokenUpdate()
    }

    /**
     * Token处理,判断Token是否需要刷新
     */
    private fun checkTokenUpdate() {
        // 先检查下Token的有效情况， 可用的话， 直接返回， 避免过多的锁操作
        val now = System.currentTimeMillis()
        // 如果Token的刷新时间都大于当前时间， 那Token肯定是有效的， 直接返回即可
        if (entity.refresh >= now) {
            return
        }
        lock.lock()
        try {
            // 如果两个值不等,那证明还有异步任务待执行,跳过本次执行
            if (entity.updateCount.get() != updateCount.get()) {
                log.warn(
                    "===> {}: AtomicInteger 值不相同, 不做更新操作, Global: {}, Entity: {}",
                    key(), updateCount.get(), entity.updateCount.get()
                )
                return
            }
            // 如果expires时间为0，证明还未初始化
            // 如果到期时间大于当前时间，那就必须先刷新
            if (entity.expires == 0L || entity.expires <= now) {
                updateCount.incrementAndGet()
                doRefresh()
                log.info("===> {}: sync refresh finish", key())
            } else if (entity.refresh <= now) {
                // 当前时间大于刷新时间，并且在有效时间内，异步刷新即可
                updateCount.incrementAndGet()
                runBlocking {
                    coroutineScope {
                        launch {
                            doRefresh()
                            log.info("===> {}: async refresh finish", key())
                        }
                    }
                }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * 从返回的json数据中解析Token并更新到entity里面
     * 允许被覆写
     */
    protected open fun parseAndUpdateToken(
        json: String, tokenKey: String = "access_token", expiresKey: String = "expires_in"
    ) {
        super.parseAndUpdateToken(entity, json, tokenKey, expiresKey)
    }

    private fun doRefresh() = try {
        requestToken()
    } finally {
        // 不管刷新结果如何,这里都必须更新刷新参数
        entity.updateCount.incrementAndGet()
    }
}
