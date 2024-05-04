package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.token_manager.DefaultTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialAccessTokenManager(
    private val config: WeiXinConfig,
    private val weiXinApiClient: OfficialWeiXinApiClient?
) : DefaultTokenManager() {

    override fun key() = "officialAccessToken"

    override fun requestToken() {
        log.info("===> weixin: request official accessToken")
        val json = weiXinApiClient?.accessToken()
            ?: NetUtils.build(WeiXinDict.WX_API_ACCESS_TOKEN.format(config.official.appId, config.official.appSecret))
                .doGet()
        parseAndUpdateToken(json)
    }
}
