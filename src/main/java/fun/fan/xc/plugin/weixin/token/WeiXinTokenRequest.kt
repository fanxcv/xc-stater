package `fun`.fan.xc.plugin.weixin.token

interface WeiXinTokenRequest {
  fun fetchToken(key: String, entity: WeiXinBaseTokenManager.TokenEntity, fn: () -> String)
}
