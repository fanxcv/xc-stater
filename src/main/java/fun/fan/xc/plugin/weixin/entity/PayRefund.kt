package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信支付申请退款
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_4
 * @author fan
 */
open class PayRefund : PayBase() {
    /**
     * 微信支付订单号
     */
    @JsonProperty("transaction_id")
    @JSONField(name = "transaction_id")
    var transactionId: String? = null

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    @JSONField(name = "out_trade_no")
    var outTradeNo: String? = null

    /**
     * 商户退款单号
     */
    @JsonProperty("out_refund_no")
    @JSONField(name = "out_refund_no")
    var outRefundNo: String? = null

    /**
     * 总金额
     */
    @JsonProperty("total_fee")
    @JSONField(name = "total_fee")
    var totalFee: Int? = null

    /**
     * 退款金额
     */
    @JsonProperty("refund_fee")
    @JSONField(name = "refund_fee")
    var refundFee: Int? = null

    /**
     * 货币类型
     */
    @JsonProperty("refund_fee_type")
    @JSONField(name = "refund_fee_type")
    var refundFeeType: String = "CNY"

    /**
     * 退款原因
     */
    @JsonProperty("refund_desc")
    @JSONField(name = "refund_desc")
    var refundDesc: String? = null

    /**
     * 退款资金来源
     */
    @JsonProperty("refund_account")
    @JSONField(name = "refund_account")
    var refundAccount: RefundAccount = RefundAccount.REFUND_SOURCE_UNSETTLED_FUNDS

    /**
     * 通知地址
     */
    @JsonProperty("notify_url")
    @JSONField(name = "notify_url")
    var notifyUrl: String? = null
}