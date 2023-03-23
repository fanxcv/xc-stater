package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.BaseAccessTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialAccessTokenManager(
    config: WeiXinConfig,
    weiXinApiClient: OfficialWeiXinApiClient?
) : BaseAccessTokenManager(config, weiXinApiClient) {
    init {
        appId = config.official.appId
        appSecret = config.official.appSecret
    }
    override fun logName() = "OfficialAccessTokenManager"
    override fun name() = "officialAccessToken"
}