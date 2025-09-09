package `fun`.fan.xc.plugin.weixin.program

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
@ConditionalOnProperty(
  prefix = "xc.weixin.mini-program",
  value = ["enable"],
  havingValue = "true",
  matchIfMissing = false
)
class ProgramAccessTokenManager(
  private val config: WeiXinConfig,
  private val request: WeiXinTokenRequest,
  private val weiXinApiClient: ProgramWeiXinApiClient?
) : WeiXinBaseTokenManager() {
  private val key = "programAccessToken"

  override fun init() {
    if (config.miniProgram.isCheckTokenWhenStart) {
      super.init()
    }
  }

  override fun key() = key

  override fun doRefresh(entity: TokenEntity) {
    request.fetchToken(key, "access_token", "expires_in", entity) { requestToken() }
  }

  fun requestToken(): String {
    log.info("===> weixin: request program accessToken")
    return weiXinApiClient?.accessToken()
      ?: NetUtils.build(WeiXinDict.WX_API_ACCESS_TOKEN.format(config.miniProgram.appId, config.miniProgram.appSecret))
        .doGet()
  }
}
