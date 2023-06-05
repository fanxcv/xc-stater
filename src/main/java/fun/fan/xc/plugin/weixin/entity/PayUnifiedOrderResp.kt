package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信统一下单接口返回
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_1&index=1
 * @author fan
 */
open class PayUnifiedOrderResp : PayBaseResp() {
    /**
     * 设备号
     */
    @JsonProperty("device_info")
    @JSONField(name = "device_info")
    var deviceInfo: String? = null

    /**
     * 交易类型
     */
    @JsonProperty("trade_type")
    @JSONField(name = "trade_type")
    var tradeType: TradeType? = null

    /**
     * 预支付交易会话标识
     */
    @JsonProperty("prepay_id")
    @JSONField(name = "prepay_id")
    var prepayId: String? = null

    /**
     * 二维码链接
     */
    @JsonProperty("code_url")
    @JSONField(name = "code_url")
    var codeUrl: String? = null
}