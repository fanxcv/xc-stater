package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.AbstractTokenManager
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.starter.exception.XcServiceException
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
) : AbstractTokenManager() {
    private val jsApiTicket = TokenEntity()

    override fun token(): String {
        checkTokenUpdate(jsApiTicket)
        return jsApiTicket.token ?: throw XcServiceException("get WeiXin jsApiTicket fail")
    }

    override fun expires(): Long {
        return jsApiTicket.expires
    }

    override fun initToken() {
        jsApiTicket.expires = 0
    }

    override fun initLock() {
        jsApiTicket.updateCount.set(Int.MIN_VALUE)
        super.updateCount.set(Int.MIN_VALUE)
        log.debug(
            "JsApiTicketManager: initLock, Global: {}, Entity: {}",
            updateCount.get(),
            jsApiTicket.updateCount.get()
        )
    }

    override fun refreshToken() {
        log.info("===> weixin: refresh WeiXin jsApiTicket")
        val json = weiXinApiClient?.jsTicket()
            ?: NetUtils.build(WeiXinDict.WX_API_JS_TICKET.format(accessTokenManager.token()))
                .doGet()
        // 判断是否正确获取到ticket了
        if (json.contains("\"ticket\"")) {
            updateToken(jsApiTicket, json, "ticket")
        } else {
            log.error("refresh WeiXin jsApiTicket fail, {}", json)
        }
    }

    override fun logName() = "JsApiTicketManager"

    override fun name() = "officialJsApiTicket"
}
