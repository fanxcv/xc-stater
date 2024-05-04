package `fun`.fan.xc.plugin.weixin.program

import `fun`.fan.xc.plugin.token_manager.DefaultTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
@ConditionalOnProperty(
    prefix = "xc.weixin.mini-program",
    value = ["enable"],
    havingValue = "true",
    matchIfMissing = false
)
class ProgramAccessTokenManager(
    private val config: WeiXinConfig,
    private val weiXinApiClient: ProgramWeiXinApiClient?
) : DefaultTokenManager() {

    override fun key() = "programAccessToken"

    override fun requestToken() {
        log.info("===> weixin: request program accessToken")
        val json = weiXinApiClient?.accessToken()
            ?: NetUtils.build(WeiXinDict.WX_API_ACCESS_TOKEN.format(config.miniProgram.appId, config.miniProgram.appSecret))
                .doGet()
        parseAndUpdateToken(json)
    }
}
