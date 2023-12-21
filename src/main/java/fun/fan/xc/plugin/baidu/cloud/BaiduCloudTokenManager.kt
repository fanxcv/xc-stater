package `fun`.fan.xc.plugin.baidu.cloud

import com.alibaba.fastjson2.JSONObject
import `fun`.fan.xc.plugin.baidu.BaiduConfig
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.utils.NetUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.baidu.cloud", value = ["enable"], havingValue = "true", matchIfMissing = false)
class BaiduCloudTokenManager(private val config: BaiduConfig) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 用于实现自旋锁
     */
    private val lock: ReentrantLock = ReentrantLock()

    /**
     * 用于记录Token更新次数
     */
    private val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)

    private val accessToken = TokenEntity()

    fun token(): String {
        checkTokenUpdate(accessToken)
        return accessToken.token ?: throw XcRunException("Baidu Cloud: get accessToken fail")
    }

    fun expires(): Long {
        return accessToken.expires
    }

    fun initToken() {
        accessToken.expires = 0
    }

    fun initLock() {
        accessToken.updateCount.set(Int.MIN_VALUE)
        updateCount.set(Int.MIN_VALUE)
        log.debug(
            "Baidu Cloud: initLock, Global: {}, Entity: {}",
            updateCount.get(), accessToken.updateCount.get()
        )
    }

    /**
     * Token处理,判断Token是否需要刷新
     */
    private fun checkTokenUpdate(entity: TokenEntity) {
        lock.lock()
        try {
            // 如果两个值不等,那证明还有异步任务待执行,跳过本次执行
            if (entity.updateCount.get() == updateCount.get()) {
                val now = System.currentTimeMillis()
                // 如果expires时间为0，证明还未初始化
                // 如果到期时间大于当前时间，那就必须先刷新
                if (entity.expires == 0L || entity.expires <= now) {
                    updateCount.incrementAndGet()
                    refresh(entity)
                    log.info("===> Baidu Cloud: token: sync refresh finish")
                } else if (entity.refresh <= now) {
                    // 当前时间大于刷新时间，并且在有效时间内，异步刷新即可
                    updateCount.incrementAndGet()
                    runBlocking {
                        coroutineScope {
                            launch {
                                refresh(entity)
                                log.info("===> Baidu Cloud: token: async refresh finish")
                            }
                        }
                    }
                }
            } else {
                log.warn(
                    "===> Baidu Cloud: token: AtomicInteger 值不相同, 不做更新操作, Global: {}, Entity: {}",
                    updateCount.get(), entity.updateCount.get()
                )
            }
        } finally {
            lock.unlock()
        }
    }

    private fun requestToken() {
        val res: JSONObject = NetUtils.build(BaiduCloudDict.BAIDU_CLOUD_ACCESS_TOKEN.format(config.cloud?.clientId, config.cloud?.clientSecret))
            .respType(JSONObject::class.java)
            .doPost()

        val time = System.currentTimeMillis()

        accessToken.token = res["access_token"] as String

        val expiresTime = res["expires_in"] as Int?
        if (expiresTime != null && expiresTime != 0) {
            // 提前十分钟就进行刷新操作
            accessToken.refresh = time + (expiresTime - 600) * 1000L
            // 提前30秒就触发同步刷新
            accessToken.expires = time + (expiresTime - 30) * 1000L
        }

        log.info("Baidu Cloud: 获取到新的Token: {}, 到期时间: {}", accessToken.token, accessToken.expires)
    }

    private fun refresh(entity: TokenEntity) = try {
        requestToken()
    } finally {
        // 不管刷新结果如何,这里都必须更新刷新参数
        entity.updateCount.incrementAndGet()
    }

    class TokenEntity {
        /**
         * token
         */
        var token: String? = null

        /**
         * 到期时间，单位ms
         */
        var expires: Long = 0

        /**
         * 下次刷新时间，单位ms
         */
        var refresh: Long = 0

        /**
         * Token更新次数
         */
        val updateCount: AtomicInteger = AtomicInteger(Int.MIN_VALUE)
        // var lastRefreshTime: String = "01-01 00:00:00"//上次刷新时间
    }
}
