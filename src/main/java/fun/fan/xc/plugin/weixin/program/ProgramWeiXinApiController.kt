package `fun`.fan.xc.plugin.weixin.program

import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.WeixinServerEnable
import `fun`.fan.xc.starter.out.R
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.*

@RestController
@ConditionalOnBean(WeixinServerEnable::class)
@RequestMapping("\${xc.weixin.server.base-path:${WeiXinDict.WX_BASE_PATH}}/program")
@ConditionalOnProperty(
    prefix = "xc.weixin.mini-program",
    value = ["enable"],
    havingValue = "true",
    matchIfMissing = false
)
class ProgramWeiXinApiController(
    private val config: WeiXinConfig,
    private val accessTokenManager: ProgramAccessTokenManager
) : InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 获取所有token
     * @return accessToken和jsTicket
     */
    @GetMapping("token")
    fun getToken(): R<Map<String, Any>> {
        return R.success(
            mapOf(
                "accessToken" to accessTokenManager.token(),
                "accessTokenExpires" to accessTokenManager.expires()
            )
        )
    }

    /**
     * 提供给客户端同步数据用的
     */
    @GetMapping("accessToken")
    fun accessToken(): R<Void> {
        return R.build<Void>()
            .set("access_token", accessTokenManager.token())
            .set("expires", accessTokenManager.expires())
    }

    override fun afterPropertiesSet() {
        val path =
            if (StringUtils.isNotBlank(config.server.basePath)) config.server.basePath else WeiXinDict.WX_BASE_PATH
        log.info("===> weixin: Program api is running, entrypoint: $path/program/{api}")
    }
}