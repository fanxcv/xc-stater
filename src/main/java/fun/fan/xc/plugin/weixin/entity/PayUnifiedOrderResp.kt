package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 微信统一下单接口返回
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_1&index=1
 * @author fan
 */
open class PayUnifiedOrderResp {
    /**
     * 返回状态码
     */
    @JsonProperty("return_code")
    @JSONField(name = "return_code")
    var returnCode: String? = null

    /**
     * 返回信息
     */
    @JsonProperty("return_msg")
    @JSONField(name = "return_msg")
    var returnMsg: String? = null

    /**
     * 小程序ID
     */
    var appid: String? = null

    /**
     * 商户号
     */
    @JsonProperty("mch_id")
    @JSONField(name = "mch_id")
    var mchId: String? = null

    /**
     * 设备号
     */
    @JsonProperty("device_info")
    @JSONField(name = "device_info")
    var deviceInfo: String? = null

    /**
     * 随机字符串
     */
    @JsonProperty("nonce_str")
    @JSONField(name = "nonce_str")
    var nonceStr: String? = null

    /**
     * 签名
     */
    var sign: String? = null

    /**
     * 业务结果
     */
    @JsonProperty("result_code")
    @JSONField(name = "result_code")
    var resultCode: String? = null

    /**
     * 错误代码
     */
    @JsonProperty("err_code")
    @JSONField(name = "err_code")
    var errCode: String? = null

    /**
     * 错误代码描述
     */
    @JsonProperty("trade_type")
    @JSONField(name = "trade_type")
    var errCodeDes: String? = null

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