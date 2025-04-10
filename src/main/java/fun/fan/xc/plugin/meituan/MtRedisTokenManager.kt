package `fun`.fan.xc.plugin.meituan

import `fun`.fan.xc.plugin.redis.Redis
import `fun`.fan.xc.starter.exception.XcServiceException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component


@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.meituan", value = ["mode"], havingValue = "redis")
class MtRedisTokenManager(
    config: MtConfig,
    private val redis: Redis
) : MtBaseTokenManager(config) {
    private val lock = "meituan:token:lock"
    private val key = "meituan:token:value"
    private var refresh = redis.hGet<Long>(key, "refresh")

    override fun refreshToken() {
        val now = System.currentTimeMillis()
        if (refresh > now) {
            return
        }
        if (!redis.tryGetDistributedLock(lock, "1", 30 * 1000)) {
            return
        }
        try {
            // 获得锁后再判断下是否被其他服务更新过了
            val v = redis.hGet<Long>(key, "refresh")
            if (v > now) {
                refresh = v
                return
            }
            val refreshToken: String = redis.hGet(key, "refreshToken")
            doRefresh(refreshToken)
        } finally {
            redis.releaseDistributedLock(lock, "1")
        }
    }

    override fun doRequest(uri: String, params: MutableMap<String, Any?>) {
        val data = requestToken(uri, params)
        val now = System.currentTimeMillis() - 10 * 1000
        val expires = now + data.getLong("expireIn") * 1000
        refresh = expires - 30 * 60 * 1000

        redis.hSet(key, "refreshToken", data.getString("refreshToken"))
        redis.hSet(key, "accessToken", data.getString("accessToken"))
        redis.hSet(key, "refresh", refresh)
        redis.hSet(key, "expires", expires)
    }

    override fun getToken(): String {
        refreshToken()
        return redis.hGet(key, "accessToken") ?: throw XcServiceException("MeiTuan: get accessToken fail")
    }
}
