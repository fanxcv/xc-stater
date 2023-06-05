package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信退款回调数据
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_16&index=10
 * @author fan
 */
open class PayRefundNotifyResp : PayBaseResp() {
    /**
     * 加密信息
     */
    @JsonProperty("req_info")
    @JSONField(name = "req_info")
    var reqInfo: String? = null

    /**
     * 解密后的返回信息
     */
    var refundInfo: RefundInfo? = null

    class RefundInfo {
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
         * 订单金额
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
         * 申请退款金额
         */
        @JsonProperty("refund_fee")
        @JSONField(name = "refund_fee")
        var refundFee: Int? = null

        /**
         * 退款金额
         */
        @JsonProperty("settlement_refund_fee")
        @JSONField(name = "settlement_refund_fee")
        var settlementRefundFee: Int? = null

        /**
         * 退款状态
         */
        @JsonProperty("refund_status")
        @JSONField(name = "refund_status")
        var refundStatus: RefundStatus? = null

        /**
         * 退款成功时间
         */
        @JsonProperty("success_time")
        @JSONField(name = "success_time")
        var successTime: String? = null

        /**
         * 退款入账账户
         */
        @JsonProperty("refund_recv_accout")
        @JSONField(name = "refund_recv_accout")
        var refundRecvAccout: String? = null

        /**
         * 退款资金来源
         */
        @JsonProperty("refund_account")
        @JSONField(name = "refund_account")
        var refundAccount: RefundAccount? = null

        /**
         * 退款发起来源
         */
        @JsonProperty("refund_request_source")
        @JSONField(name = "refund_request_source")
        var refundRequestSource: RefundRequestSource? = null

        /**
         * 用户退款金额
         */
        @JsonProperty("cash_refund_fee")
        @JSONField(name = "cash_refund_fee")
        var cashRefundFee: Int? = null
    }
}