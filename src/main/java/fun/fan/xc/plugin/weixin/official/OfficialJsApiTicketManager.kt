package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.token.WeiXinBaseTokenManager
import `fun`.fan.xc.plugin.weixin.token.WeiXinTokenRequest
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialJsApiTicketManager(
  private val request: WeiXinTokenRequest,
  private val weiXinApiClient: OfficialWeiXinApiClient?,
  private val accessTokenManager: OfficialAccessTokenManager
) : WeiXinBaseTokenManager() {
  private val key = "officialJsApiTicket"
  override fun init() {
    // 重写是为了禁止 禁止启动时获取js ticket
  }

  override fun key() = key

  override fun doRefresh(entity: TokenEntity) {
    request.fetchToken(key, "ticket", "expires_in", entity) { requestToken() }
  }

  /**
   * {"errcode":0,"errmsg":"ok","ticket":"O3SMpm8bG7kJnF36aXbe83eu82txrSCxLjqh0_HNKTvgOFk2GHik_LTBoNpiTz6pbwAft8Or83E71HCRCCDh_w","expires_in":7200}
   */
  fun requestToken(): String {
    log.info("===> weixin: request jsApiTicket")
    return weiXinApiClient?.jsTicket()
      ?: NetUtils.build(WeiXinDict.WX_API_JS_TICKET.format(accessTokenManager.token()))
        .doGet()
  }
}
