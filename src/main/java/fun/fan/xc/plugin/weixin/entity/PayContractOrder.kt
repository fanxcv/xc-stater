package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-支付中签约
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_5.shtml
 * @author fan
 */
open class PayContractOrder : PayUnifiedOrder() {

    /**
     * 签约商户号
     */
    @JsonProperty("contract_mchid")
    @JSONField(name = "contract_mchid")
    var contractMchId: String? = null

    /**
     * 签约appid
     */
    @JsonProperty("contract_appid")
    @JSONField(name = "contract_appid")
    var contractAppid: String? = null

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
    var requestSerial: String? = null

    /**
     * 用户账户展示名称
     */
    @JsonProperty("contract_display_account")
    @JSONField(name = "contract_display_account")
    var contractDisplayAccount: String? = null

    /**
     * 签约信息通知url
     */
    @JsonProperty("contract_notify_url")
    @JSONField(name = "contract_notify_url")
    var contractNotifyUrl: String? = null





    /**
     * 货币类型
     */
    @JsonProperty("fee_type")
    @JSONField(name = "fee_type")
    override var feeType: String? = null
}
