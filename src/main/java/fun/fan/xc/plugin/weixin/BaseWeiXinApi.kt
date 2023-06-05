package `fun`.fan.xc.plugin.weixin

import com.wechat.pay.java.core.Config
import com.wechat.pay.java.core.RSAAutoCertificateConfig
import com.wechat.pay.java.core.RSAConfig
import `fun`.fan.xc.plugin.weixin.entity.WXAccessTokenCommonResp
import `fun`.fan.xc.starter.utils.Dict
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.util.Assert

open class BaseWeiXinApi(
    private val config: WeiXinConfig,
    private val accessTokenManager: TokenManager
) {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    protected var pay: WeiXinConfig.Pay? = null
    lateinit var appSecret: String
    lateinit var appId: String

    val autoCertConfig: Config by lazy {
        Assert.notNull(pay, "微信支付配置不能为空")
        val resource = DefaultResourceLoader()
        RSAAutoCertificateConfig.Builder()
            .privateKeyFromPath(resource.getResource(pay!!.apiKeyPath).file.path)
            .merchantSerialNumber(pay!!.apiKeySerialNo)
            .apiV3Key(pay!!.apiV3Key)
            .merchantId(pay!!.mchId)
            .build()
    }

    val wxPayConfig:RSAConfig by lazy {
        Assert.notNull(pay, "微信支付配置不能为空")
        val resource = DefaultResourceLoader()
        RSAConfig.Builder()
            .wechatPayCertificatesFromPath(resource.getResource(pay!!.wechatPayCertPath).file.path)
            .privateKeyFromPath(resource.getResource(pay!!.apiKeyPath).file.path)
            .merchantSerialNumber(pay!!.apiKeySerialNo)
            .merchantId(pay!!.mchId)
            .build();
    }

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