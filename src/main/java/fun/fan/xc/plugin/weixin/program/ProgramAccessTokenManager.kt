package `fun`.fan.xc.plugin.weixin.program

import `fun`.fan.xc.plugin.weixin.BaseAccessTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
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
    config: WeiXinConfig,
    weiXinApiClient: ProgramWeiXinApiClient?
) : BaseAccessTokenManager(config, weiXinApiClient) {
    init {
        appId = config.miniProgram.appId
        appSecret = config.miniProgram.appSecret
    }
    override fun logName() = "ProgramAccessTokenManager"
    override fun name() = "programAccessToken"
}