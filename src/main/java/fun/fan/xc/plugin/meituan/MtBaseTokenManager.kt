package `fun`.fan.xc.plugin.meituan

import com.alibaba.fastjson2.JSONObject
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.util.Assert

abstract class MtBaseTokenManager(
    private val config: MtConfig
) : MtTokenManager {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun initToken(code: String) {
        mutableMapOf<String, Any?>(
            "developerId" to config.developerId,
            "businessId" to config.businessId,
            "timestamp" to System.currentTimeMillis() / 1000,
            "grantType" to "authorization_code",
            "charset" to "UTF-8",
            "code" to code,
        ).let { doRequest(MtUtils.TOKEN_URI, it) }
    }

    protected fun doRefresh(refreshToken: String?) {
        mutableMapOf<String, Any?>(
            "developerId" to config.developerId,
            "businessId" to config.businessId,
            "timestamp" to System.currentTimeMillis() / 1000,
            "refreshToken" to refreshToken,
            "grantType" to "refresh_token",
            "charset" to "UTF-8",
            "scope" to "all"
        ).let { doRequest(MtUtils.REFRESH_URI, it) }
    }

    protected fun requestToken(uri: String, params: MutableMap<String, Any?>): JSONObject {
        params["sign"] = MtUtils.sign(params, config.signKey)
        val res = NetUtils.build(uri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .addParams(params)
            .doPost<JSONObject>(JSONObject::class.java)
        Assert.isTrue(res.getIntValue("code") == 0, "请求Token失败了, response: $res")
        val data = res.getObject("data", JSONObject::class.java)
        Assert.isTrue(data != null, "请求Token失败了, response: $res")
        return data
    }

    abstract fun doRequest(uri: String, params: MutableMap<String, Any?>)
}
