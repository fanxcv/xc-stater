package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-申请解约请求体
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_9.shtml
 * @author fan
 */
open class PayDeleteContract : PayBase() {

    /**
     * 委托代扣协议id
     */
    @JsonProperty("contract_id")
    @JSONField(name = "contract_id")
    var contractId: String? = null

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
     * 解约备注
     */
    @JsonProperty("contract_termination_remark")
    @JSONField(name = "contract_termination_remark")
    var contractTerminationRemark: String? = null

    /**
     * 版本号
     */
    var version: String = "1.0"

    /**
     * 随机字符串, 需要从base中移除的字段, 避免参与签名计算
     */
    @JsonProperty("nonce_str")
    @JSONField(name = "nonce_str")
    override var nonceStr: String? = null

    /**
     * 签名类型, 需要从base中移除的字段, 避免参与签名计算
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    override var signType: SignType? = null
}
