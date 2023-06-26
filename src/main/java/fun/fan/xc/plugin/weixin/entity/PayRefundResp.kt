package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信支付申请退款返回
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_4
 * @author fan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
open class PayRefundResp : PayBaseResp() {
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
     * 微信退款单号
     */
    @JsonProperty("refund_id")
    @JSONField(name = "refund_id")
    var refundId: String? = null

    /**
     * 退款金额
     */
    @JsonProperty("refund_fee")
    @JSONField(name = "refund_fee")
    var refundFee: Int? = null

    /**
     * 应结退款金额
     */
    @JsonProperty("settlement_refund_fee")
    @JSONField(name = "settlement_refund_fee")
    var settlementRefundFee: Int? = null

    /**
     * 标价金额
     */
    @JsonProperty("total_fee")
    @JSONField(name = "total_fee")
    var totalFee: Int? = null

    /**
     * 应结订单金额
     */
    @JsonProperty("settlement_total_fee")
    @JSONField(name = "settlement_total_fee")
    var settlementTotalFee: Int? = null

    /**
     * 标价币种
     */
    @JsonProperty("fee_type")
    @JSONField(name = "fee_type")
    var feeType: String = "CNY"

    /**
     * 现金支付金额
     */
    @JsonProperty("cash_fee")
    @JSONField(name = "cash_fee")
    var cashFee: Int? = null

    /**
     * 现金支付币种
     */
    @JsonProperty("cash_fee_type")
    @JSONField(name = "cash_fee_type")
    var cashFeeType: String = "CNY"

    /**
     * 现金退款金额
     */
    @JsonProperty("cash_refund_fee")
    @JSONField(name = "cash_refund_fee")
    var cashRefundFee: Int? = null
}