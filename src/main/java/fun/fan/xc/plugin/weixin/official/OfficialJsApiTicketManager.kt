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
    request.fetchToken(key, entity) { requestToken() }
  }

  fun requestToken(): String {
    log.info("===> weixin: request jsApiTicket")
    return weiXinApiClient?.jsTicket()
      ?: NetUtils.build(WeiXinDict.WX_API_JS_TICKET.format(accessTokenManager.token()))
        .doGet()
  }
}
