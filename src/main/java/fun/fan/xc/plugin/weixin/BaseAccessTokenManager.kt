package `fun`.fan.xc.plugin.weixin

import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.utils.NetUtils

abstract class BaseAccessTokenManager(
    private val config: WeiXinConfig,
    private val weiXinApiClient: WeiXinApiClient?
) : AbstractTokenManager() {
    protected val accessToken = TokenEntity()
    protected lateinit var appSecret: String
    protected lateinit var appId: String

    override fun token(): String {
        checkTokenUpdate(accessToken)
        return accessToken.token ?: throw XcServiceException("${logName()} get WeiXin accessToken fail")
    }

    override fun expires(): Long {
        return accessToken.expires
    }

    override fun initToken() {
        accessToken.expires = 0
    }

    override fun initLock() {
        accessToken.updateCount.set(Int.MIN_VALUE)
        super.updateCount.set(Int.MIN_VALUE)
        log.debug(
            "${logName()}: initLock, Global: {}, Entity: {}",
            updateCount.get(),
            accessToken.updateCount.get()
        )
    }

    override fun refreshToken() {
        log.info("===> weixin: ${logName()} refresh WeiXin accessToken")
        val json = weiXinApiClient?.accessToken()
            ?: NetUtils.build(WeiXinDict.WX_API_ACCESS_TOKEN.format(appId, appSecret))
                .doGet()
        // 判断是否正确获取到accessToken了
        if (json.contains("\"access_token\"")) {
            updateToken(accessToken, json, "access_token")
        } else {
            log.error("${logName()} refresh WeiXin accessToken fail, {}", json)
        }
    }
}
