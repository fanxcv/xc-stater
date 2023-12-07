package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-支付中签约
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_5.shtml
 * @author fan
 */
open class PayContractOrderResp : PayUnifiedOrderResp() {

    /**
     * 预签约结果
     */
    @JsonProperty("contract_result_code")
    @JSONField(name = "contract_result_code")
    var contractResultCode: String? = null

    /**
     * 预签约错误代码
     */
    @JsonProperty("contract_err_code")
    @JSONField(name = "contract_err_code")
    var contractErrCode: String? = null

    /**
     * 预签约错误描述
     */
    @JsonProperty("contract_err_code_des")
    @JSONField(name = "contract_err_code_des")
    var contractErrCodeDes: String? = null

    /**
     * 请求序列号
     */
    @JsonProperty("request_serial")
    @JSONField(name = "request_serial")
    var requestSerial: String? = null

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
     * 用户账户展示名称
     */
    @JsonProperty("contract_display_account")
    @JSONField(name = "contract_display_account")
    var contractDisplayAccount: String? = null

    /**
     * 支付跳转链接
     */
    @JsonProperty("mweb_url")
    @JSONField(name = "mweb_url")
    var mWebUrl: String? = null

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    @JSONField(name = "out_trade_no")
    var outTradeNo: String? = null
}
