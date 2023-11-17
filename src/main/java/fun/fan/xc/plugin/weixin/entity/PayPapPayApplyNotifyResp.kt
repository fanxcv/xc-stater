package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-扣款结果通知
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter4_2.shtml
 * @author fan
 */
open class PayPapPayApplyNotifyResp : PayBaseResp() {
    /**
     * 设备号
     */
    @JsonProperty("device_info")
    @JSONField(name = "device_info")
    var deviceInfo: String? = null

    /**
     * 用户标识
     */
    @JsonProperty("openid")
    @JSONField(name = "openid")
    var openId: String? = null

    /**
     * 用户子标识
     */
    @JsonProperty("sub_openid")
    @JSONField(name = "sub_openid")
    var subOpenId: String? = null

    /**
     * 是否关注公众账号
     */
    @JsonProperty("is_subscribe")
    @JSONField(name = "is_subscribe")
    var isSubscribe: String? = null

    /**
     * 付款银行
     */
    @JsonProperty("bank_type")
    @JSONField(name = "bank_type")
    var bankType: String? = null

    /**
     * 订单金额
     */
    @JsonProperty("total_fee")
    @JSONField(name = "total_fee")
    var totalFee: Int? = null

    /**
     * 货币种类
     */
    @JsonProperty("fee_type")
    @JSONField(name = "fee_type")
    var feeType: String? = null

    /**
     * 现金支付金额
     */
    @JsonProperty("cash_fee")
    @JSONField(name = "cash_fee")
    var cashFee: Int? = null

    /**
     * 现金支付货币类型
     */
    @JsonProperty("cash_fee_type")
    @JSONField(name = "cash_fee_type")
    var cashFeeType: String? = null

    /**
     * 交易状态
     */
    @JsonProperty("trade_state")
    @JSONField(name = "trade_state")
    var tradeState: String? = null

    /**
     * 总代金券金额
     */
    @JsonProperty("coupon_fee")
    @JSONField(name = "coupon_fee")
    var couponFee: Int? = null

    /**
     * 代金券使用数量
     */
    @JsonProperty("coupon_count")
    @JSONField(name = "coupon_count")
    var couponCount: Int? = null

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
     * 商家数据包
     */
    var attach: String? = null

    /**
     * 支付完成时间
     */
    @JsonProperty("time_end")
    @JSONField(name = "time_end")
    var timeEnd: String? = null

    /**
     * 委托代扣协议id
     */
    @JsonProperty("contract_id")
    @JSONField(name = "contract_id")
    var contractId: String? = null

    /**
     * 子商户appid
     */
    @JsonProperty("sub_appid")
    @JSONField(name = "sub_appid")
    var subAppid: String? = null

    /**
     * 子商户id
     */
    @JsonProperty("sub_mch_id")
    @JSONField(name = "sub_mch_id")
    var subMchId: String? = null
}
