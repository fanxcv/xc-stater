package `fun`.fan.xc.plugin.weixin

import `fun`.fan.xc.plugin.weixin.entity.WXAccessTokenCommonResp
import `fun`.fan.xc.starter.utils.Dict
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class BaseWeiXinApi(
    private val config: WeiXinConfig,
    private val accessTokenManager: TokenManager
) {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    protected lateinit var appSecret: String
    protected lateinit var appId: String

    /**
     * 获取微信AccessToken
     */
    fun getAccessToken(): String = accessTokenManager.token()

    /**
     * 获取稳定版接口调用凭据
     */
    fun getStableAccessToken(forceRefresh: Boolean = false): String {
        val builder = NetUtils.build()
            .url(WeiXinDict.WX_API_STABLE_ACCESS_TOKEN)
            .addParam("grant_type", "client_credential")
            .addParam("secret", appSecret)
            .addParam("appid", appId)
        if (forceRefresh) {
            builder.addParam("forceRefresh", true)
        }
        val resp: WXAccessTokenCommonResp = builder.doPost(WXAccessTokenCommonResp::class.java)
        return resp.accessToken ?: Dict.BLANK
    }

    // fun sendUniformMessage() {
    //     NetUtils.build(WeiXinDict.WX_API_MESSAGE_UNIFORM.format(accessTokenManager.token()))
    //         .body()
    // }
}