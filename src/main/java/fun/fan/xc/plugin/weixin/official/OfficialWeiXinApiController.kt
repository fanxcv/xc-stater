package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.WeixinServerEnable
import `fun`.fan.xc.starter.exception.BussinessException
import `fun`.fan.xc.starter.out.R
import jakarta.validation.constraints.NotBlank
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@ConditionalOnBean(WeixinServerEnable::class)
@RequestMapping("\${xc.weixin.server.base-path:${WeiXinDict.WX_BASE_PATH}}/official")
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialWeiXinApiController(
    private val config: WeiXinConfig,
    private val weiXinApi: OfficialWeiXinApi,
    private val accessTokenManager: OfficialAccessTokenManager,
    private val jsApiTicketManager: OfficialJsApiTicketManager
) : InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 服务器验证接口
     */
    @Validated
    @GetMapping("signature")
    fun signature(
        @RequestParam @NotBlank nonce: String,
        @RequestParam @NotBlank echostr: String,
        @RequestParam @NotBlank timestamp: String,
        @RequestParam @NotBlank signature: String
    ): String {
        val res = weiXinApi.checkSignature(nonce, timestamp, signature, echostr)
        return if (res == echostr) res else throw BussinessException()
    }

    /**
     * 获取所有token
     * @return accessToken和jsTicket
     */
    @GetMapping("token")
    fun getToken(): R<Map<String, Any>> {
        return R.success(
            mapOf(
                "jsTicket" to jsApiTicketManager.token(),
                "jsTicketExpires" to jsApiTicketManager.expires(),
                "accessToken" to accessTokenManager.token(),
                "accessTokenExpires" to accessTokenManager.expires()
            )
        )
    }

    /**
     * 初始化刷新锁, 避免死锁必须重启服务器
     */
    @PutMapping("initLock")
    fun initLock() {
        accessTokenManager.initLock()
        jsApiTicketManager.initLock()
    }

    /**
     * 强制触发Token刷新
     */
    @PutMapping("initToken")
    fun initToken() {
        accessTokenManager.initToken()
        jsApiTicketManager.initToken()
    }

    /**
     * 提供给客户端同步数据用的
     */
    @GetMapping("accessToken")
    fun accessToken(): Map<String, Any?> {
        return mapOf(
            "code" to 0,
            "access_token" to accessTokenManager.token(),
            "expires" to accessTokenManager.expires()
        )
    }

    /**
     * 提供给客户端同步数据用的
     */
    @GetMapping("jsTicket")
    fun jsTicket(): Map<String, Any?> {
        return mapOf(
            "code" to 0,
            "ticket" to jsApiTicketManager.token(),
            "expires" to jsApiTicketManager.expires()
        )
    }

    override fun afterPropertiesSet() {
        val path =
            if (StringUtils.isNotBlank(config.server.basePath)) config.server.basePath else WeiXinDict.WX_BASE_PATH
        log.info("===> weixin: Official api is running, entrypoint: $path/official/{api}")
    }
}