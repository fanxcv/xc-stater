package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-签约、解约结果通知
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_6.shtml
 * @author fan
 */
open class PayAddOrDelContractNotifyResp : PayBaseResp() {

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
     * 用户标识
     */
    @JsonProperty("openid")
    @JSONField(name = "openid")
    var openId: String? = null

    /**
     * 变更类型
     */
    @JsonProperty("change_type")
    @JSONField(name = "change_type")
    var changeType: String? = null

    /**
     * 操作时间
     */
    @JsonProperty("operate_time")
    @JSONField(name = "operate_time")
    var operateTime: String? = null

    /**
     * 合同到期时间
     */
    @JsonProperty("contract_expired_time")
    @JSONField(name = "contract_expired_time")
    var contractExpiredTime: String? = null

    /**
     * 合同终止方式
     */
    @JsonProperty("contract_termination_mode")
    @JSONField(name = "contract_termination_mode")
    var contractTerminationMode: Int? = null

    /**
     * 请求序列号
     */
    @JsonProperty("request_serial")
    @JSONField(name = "request_serial")
    var requestSerial: Long? = null
}
