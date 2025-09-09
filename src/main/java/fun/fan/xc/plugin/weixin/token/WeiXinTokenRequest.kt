package `fun`.fan.xc.plugin.weixin.token

interface WeiXinTokenRequest {
  fun fetchToken(key: String, tokenKey: String, expiresKey: String, entity: WeiXinBaseTokenManager.TokenEntity, fn: () -> String)
}
