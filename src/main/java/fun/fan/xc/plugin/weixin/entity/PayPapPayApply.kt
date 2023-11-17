package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 委托代扣-申请扣款请求体
 * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_8.shtml
 * @author fan
 */
open class PayPapPayApply: PayBase() {
    /**
     * 商品描述
     */
    var body: String = ""

    /**
     * 商品详情
     */
    var detail:  String? = null

    /**
     * 附加数据
     */
    var attach: String? = null

    /**
     * 商户订单号
     */
    @JsonProperty("out_trade_no")
    @JSONField(name = "out_trade_no")
    var outTradeNo: String? = null

    /**
     * 货币类型
     */
    @JsonProperty("fee_type")
    @JSONField(name = "fee_type")
    var feeType: String = "CNY"

    /**
     * 总金额
     */
    @JsonProperty("total_fee")
    @JSONField(name = "total_fee")
    var totalFee: Int? = null

    /**
     * 终端IP
     */
    @JsonProperty("spbill_create_ip")
    @JSONField(name = "spbill_create_ip")
    var spBillCreateIp: String? = null

    /**
     * 商品标记
     */
    @JsonProperty("goods_tag")
    @JSONField(name = "goods_tag")
    var goodsTag: String? = null

    /**
     * 通知地址
     */
    @JsonProperty("notify_url")
    @JSONField(name = "notify_url")
    var notifyUrl: String? = null

    /**
     * 交易类型
     */
    @JsonProperty("trade_type")
    @JSONField(name = "trade_type")
    var tradeType: TradeType = TradeType.PAP

    /**
     * 委托代扣协议id
     */
    @JsonProperty("contract_id")
    @JSONField(name = "contract_id")
    var contractId: String? = null

    /**
     * 签名类型, 需要从base中移除的字段, 避免参与签名计算
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    override var signType: SignType? = null
}
