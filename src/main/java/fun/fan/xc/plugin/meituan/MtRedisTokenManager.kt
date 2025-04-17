package `fun`.fan.xc.plugin.meituan

import com.alibaba.fastjson2.JSONObject
import `fun`.fan.xc.plugin.redis.Redis
import `fun`.fan.xc.starter.exception.XcServiceException
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component


@Lazy
@Component
class MtRedisTokenManager(
    config: MtConfig,
    private val redis: Redis
) : MtBaseTokenManager(config) {
    private val lock = "xc:meituan:token:lock:"
    private val key = "xc:meituan:token:value:"
    private val map = HashMap<String, Long>()

    override fun refreshToken(key: String) {
        val now = System.currentTimeMillis()

        if (map[key] == null) {
            map[key] = redis.hGet(this.key + key, "refresh") ?: 0L
        }

        if ((map[key] ?: 0L) > now) {
            return
        }

        // 先尝试获取锁，获取不到就直接返回
        if (!redis.tryGetDistributedLock(lock + key, "1", 30 * 1000)) {
            return
        }

        try {
            // 获得锁后再判断下是否被其他服务更新过了
            val v = redis.hGet<Long>(this.key + key, "refresh")
            if (v > now) {
                map[key] = v
                return
            }
            val refreshToken: String = redis.hGet(this.key + key, "refreshToken")
            doRefresh(refreshToken, key)
        } finally {
            redis.releaseDistributedLock(lock + key, "1")
        }
    }

    override fun doRequest(key: String?, uri: String, params: MutableMap<String, Any?>): JSONObject {
        val data = requestToken(uri, params)
        val now = System.currentTimeMillis() - 10 * 1000
        val expires = now + data.getLong("expireIn") * 1000
        val refresh = expires - 30 * 60 * 1000

        val k = this.key + (key ?: data.getString("opBizCode"))

        redis.hSet(k, "refreshToken", data.getString("refreshToken"))
        redis.hSet(k, "accessToken", data.getString("accessToken"))
        redis.hSet(k, "refresh", refresh)
        redis.hSet(k, "expires", expires)
        return data
    }

    override fun getToken(key: String): String {
        refreshToken(key)
        return redis.hGet(this.key + key, "accessToken") ?: throw XcServiceException("MeiTuan: get accessToken fail")
    }
}
