package `fun`.fan.xc.plugin.meituan

import `fun`.fan.xc.starter.exception.XcServiceException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock


@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.meituan", value = ["mode"], havingValue = "local")
class MtLocalTokenManager(config: MtConfig) : MtBaseTokenManager(config) {
    private val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)
    private val entity: MtTokenEntity = MtTokenEntity()
    private val lock: ReentrantLock = ReentrantLock()

    override fun refreshToken() {
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
                log.warn("===> MeiTuan: AtomicInteger 值不相同, 不做更新操作, Global: {}, Entity: {}", updateCount.get(), entity.updateCount.get())
                return
            }
            // 如果expires时间为0，证明还未初始化
            // 如果到期时间大于当前时间，那就必须先刷新
            if (entity.expires == 0L || entity.expires <= now) {
                updateCount.incrementAndGet()
                doRefresh(entity.refreshToken)
                log.info("===> MeiTuan: sync refresh finish")
            } else if (entity.refresh <= now) {
                // 当前时间大于刷新时间，并且在有效时间内，异步刷新即可
                updateCount.incrementAndGet()
                runBlocking {
                    coroutineScope {
                        launch {
                            doRefresh(entity.refreshToken)
                            log.info("===> MeiTuan: async refresh finish")
                        }
                    }
                }
            }
        } finally {
            lock.unlock()
        }
    }

    override fun doRequest(uri: String, params: MutableMap<String, Any?>) {
        val data = requestToken(uri, params)
        val now = System.currentTimeMillis() - 10 * 1000
        entity.refreshToken = data.getString("refreshToken")
        entity.accessToken = data.getString("accessToken")
        entity.expires = now + data.getLong("expireIn") * 1000
        entity.refresh = entity.expires - 30 * 60 * 1000
    }

    override fun getToken(): String {
        refreshToken()
        return entity.accessToken ?: throw XcServiceException("MeiTuan: get accessToken fail")
    }

    data class MtTokenEntity(
        /**
         * 到期时间，单位ms
         */
        var expires: Long = 0,
        /**
         * 下次刷新时间，单位ms
         */
        var refresh: Long = 0,
        var accessToken: String? = null,
        var refreshToken: String? = null,
        val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)
    )
}
