package `fun`.fan.xc.plugin.weixin.token

import `fun`.fan.xc.plugin.weixin.WeiXinUtils

/**
 * 基于AtomicInteger实现的TokenManager, 仅适用于单机部署使用
 */
class WeiXinLocalTokenRequest : WeiXinTokenRequest {
  override fun fetchToken(key: String, entity: WeiXinBaseTokenManager.TokenEntity, fn: () -> String) {
    WeiXinUtils.parseAndUpdateToken(entity, key, fn())
  }
}
