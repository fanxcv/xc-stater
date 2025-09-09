package `fun`.fan.xc.plugin.weixin.token

import `fun`.fan.xc.plugin.redis.Redis
import `fun`.fan.xc.plugin.weixin.WeiXinUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary

/**
 * 作为客户端的服务, 不能从redis中获取Token, 强制走cs模式
 */
@Primary
@ConditionalOnBean(Redis::class)
@ConditionalOnProperty(name = ["xc.weixin.client.enable"], havingValue = "false", matchIfMissing = true)
class WeiXinRedisTokenRequest(private val redis: Redis) : WeiXinTokenRequest {
  private val lock = "xc:weixin:token:lock:"
  private val key = "xc:weixin:token:value:"

  override fun fetchToken(key: String, tokenKey: String, expiresKey: String, entity: WeiXinBaseTokenManager.TokenEntity, fn: () -> String) {
    val k = this.key + key

    if (checkCacheToken(k, entity)) {
      return
    }

    // 先尝试获取锁，获取不到就直接返回
    if (!redis.tryGetDistributedLock(lock + key, "1", 30 * 1000)) {
      return
    }

    try {
      // 获得锁后再判断下是否被其他服务更新过了
      if (checkCacheToken(k, entity)) {
        return
      }
      WeiXinUtils.parseAndUpdateToken(entity, key, tokenKey, expiresKey, fn())
      redis.hSet(k, "accessToken", entity.token)
      redis.hSet(k, "expires", entity.expires)
      redis.hSet(k, "refresh", entity.refresh)
    } finally {
      redis.releaseDistributedLock(lock + key, "1")
    }
  }

  private fun checkCacheToken(key: String, entity: WeiXinBaseTokenManager.TokenEntity): Boolean {
    // 先判断redis中的缓存是否可用
    val refresh = redis.hGet(key, "refresh") ?: 0L
    if (refresh > System.currentTimeMillis()) {
      // 如果redis中保存的时间大于当前时间, 则证明被其他服务更新过了
      entity.token = redis.hGet(key, "accessToken")
      entity.expires = redis.hGet(key, "expires") ?: 0L
      entity.refresh = redis.hGet(key, "refresh") ?: 0L
      return true
    } else {
      return false
    }
  }
}
