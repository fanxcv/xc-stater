package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信下单回调数据
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_7
 * @author fan
 */
open class PayNotifyResp : PayBaseResp() {
    /**
     * 设备号
     */
    @JsonProperty("device_info")
    @JSONField(name = "device_info")
    var deviceInfo: String? = null

    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    var signType: SignType = SignType.MD5

    /**
     * 用户标识
     */
    @JsonProperty("openid")
    @JSONField(name = "openid")
    var openId: String? = null

    /**
     * 是否关注公众账号
     */
    @JsonProperty("is_subscribe")
    @JSONField(name = "is_subscribe")
    var isSubscribe: String? = null

    /**
     * 交易类型
     */
    @JsonProperty("trade_type")
    @JSONField(name = "trade_type")
    var tradeType: TradeType? = null

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
     * 应结订单金额
     */
    @JsonProperty("settlement_total_fee")
    @JSONField(name = "settlement_total_fee")
    var settlementTotalFee: Int? = null

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
}