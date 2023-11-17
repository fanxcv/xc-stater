package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-申请解约返回
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_9.shtml
 * @author fan
 */
open class PayDeleteContractResp : PayBaseResp() {

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
}
