package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.WeiXinConfig
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
class OfficialAccessTokenManager(
  private val config: WeiXinConfig,
  private val request: WeiXinTokenRequest,
  private val weiXinApiClient: OfficialWeiXinApiClient?
) : WeiXinBaseTokenManager() {
  private val key = "officialAccessToken"

  override fun init() {
    if (config.official.isCheckTokenWhenStart) {
      super.init()
    }
  }

  override fun key() = key

  override fun doRefresh(entity: TokenEntity) {
    request.fetchToken(key, "access_token", "expires_in", entity) { requestToken() }
  }

  fun requestToken(): String {
    log.info("===> weixin: request official accessToken")
    return weiXinApiClient?.accessToken()
      ?: NetUtils.build(WeiXinDict.WX_API_ACCESS_TOKEN.format(config.official.appId, config.official.appSecret))
        .doGet()
  }
}
