package `fun`.fan.xc.plugin.token_manager

import com.alibaba.fastjson2.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * 基于AtomicInteger实现的TokenManager, 仅适用于单机部署使用
 */
abstract class BaseTokenManager : TokenManager, InitializingBean {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun afterPropertiesSet() {
        this.init()
    }

    /**
     * 从返回的json数据中解析Token并更新到entity里面
     * 允许被覆写
     */
    protected fun parseAndUpdateToken(
        entity: TokenManager.BaseTokenEntity, json: String, tokenKey: String, expiresKey: String
    ) {
        // 先判断是否正确获取到tokenKey了
        if (!json.contains(tokenKey)) {
            log.error("{}: 请求Token失败了, response: {}", key(), json)
            return
        }

        val time = System.currentTimeMillis()
        val map: JSONObject = JSONObject.parse(json)
        entity.token = map.getString(tokenKey)

        // map["expires"]是处理client端的
        val expiresTime = map.getLong(expiresKey) ?: map.getLong("expires")
        if (expiresTime == null || expiresTime == 0L) {
            log.error("{}: 获取到的Token无效: {}", key(), json)
            return
        }

        // 提前30秒就触发同步刷新
        entity.expires = time + (expiresTime - 30L) * 1000L
        // 提前十分钟就进行刷新操作
        entity.refresh = time + (expiresTime - 600L) * 1000L
        log.info("{}: 获取到新的Token: {}, 到期时间: {}", key(), entity.token, entity.expires)
    }

    /**
     * 执行token刷新的方法
     * 获取到Token后需要调用@[parseAndUpdateToken]进行解析
     */
    protected abstract fun requestToken()
}
