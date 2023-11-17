package `fun`.fan.xc.plugin.weixin.entity

import cn.hutool.core.lang.UUID
import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.starter.exception.XcRunException

open class PayBase {
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
     * 随机字符串
     */
    @JsonProperty("nonce_str")
    @JSONField(name = "nonce_str")
    open var nonceStr: String? = UUID.fastUUID().toString(true)

    /**
     * 签名
     */
    var sign: String? = null

    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    open var signType: SignType? = SignType.MD5
        set(value) {
            if (value != null && value != SignType.MD5) {
                throw XcRunException("不支持的签名类型")
            }
            field = value
        }
}
