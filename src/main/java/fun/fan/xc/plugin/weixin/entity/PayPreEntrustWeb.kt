package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.starter.exception.XcServiceException

open class PayPreEntrustWeb {
    /**
     * 小程序ID
     */
    var appid: String? = null

    /**
     * 商户号
     */
    @JsonProperty("mch_id")
    @JSONField(name = "mch_id")
    var mchId: String? = null

    /**
     * 模板id
     */
    @JsonProperty("plan_id")
    @JSONField(name = "plan_id")
    var planId: String? = null

    /**
     * 签约协议号
     */
    @JsonProperty("contract_code")
    @JSONField(name = "contract_code")
    var contractCode: String? = null

    /**
     * 请求序列号
     */
    @JsonProperty("request_serial")
    @JSONField(name = "request_serial")
    var requestSerial: Int? = null

    /**
     * 用户账户展示名称
     */
    @JsonProperty("contract_display_account")
    @JSONField(name = "contract_display_account")
    var contractDisplayAccount: String? = null

    /**
     * 通知地址
     */
    @JsonProperty("notify_url")
    @JSONField(name = "notify_url")
    var notifyUrl: String? = null

    /**
     * 版本号
     */
    var version: String = "1.0"

    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    var signType: SignType = SignType.MD5
        set(value) {
            if (value != SignType.MD5) {
                throw XcServiceException("不支持的签名类型")
            }
            field = value
        }

    /**
     * 签名
     */
    var sign: String? = null

    /**
     * 时间戳
     */
    var timestamp: Long? = null

    /**
     * 返回app
     */
    @JsonProperty("return_app")
    @JSONField(name = "return_app")
    var returnApp: String? = null
}
