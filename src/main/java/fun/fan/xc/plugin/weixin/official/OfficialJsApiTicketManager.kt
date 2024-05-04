package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.token_manager.DefaultTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialJsApiTicketManager(
    private val weiXinApiClient: OfficialWeiXinApiClient?,
    private val accessTokenManager: OfficialAccessTokenManager
) : DefaultTokenManager() {

    override fun key() = "officialJsApiTicket"

    override fun requestToken() {
        log.info("===> weixin: request jsApiTicket")
        val json = weiXinApiClient?.jsTicket()
            ?: NetUtils.build(WeiXinDict.WX_API_JS_TICKET.format(accessTokenManager.token()))
                .doGet()
        parseAndUpdateToken(json, "ticket", "expires_in")
    }
}
