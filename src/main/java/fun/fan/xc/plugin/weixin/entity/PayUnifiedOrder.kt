package `fun`.fan.xc.plugin.weixin.entity

import cn.hutool.core.lang.UUID
import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.starter.exception.XcRunException

/**
 * 微信统一下单接口
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_1&index=1
 * @author fan
 */
open class PayUnifiedOrder {
    /**
     * 公众账号ID
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
    var deviceInfo: String = "JSAPI"

    /**
     * 随机字符串
     */
    @JsonProperty("nonce_str")
    @JSONField(name = "nonce_str")
    var nonceStr: String = UUID.fastUUID().toString(true)

    /**
     * 签名
     */
    var sign: String? = null

    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    @JSONField(name = "sign_type")
    var signType: SignType = SignType.MD5
        set(value) {
            if (value != SignType.MD5) {
                throw XcRunException("不支持的签名类型")
            }
            field = value
        }

    /**
     * 商品描述
     */
    var body: String? = null

    /**
     * 商品详情
     */
    var detail: String? = null

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
     * 交易起始时间
     */
    @JsonProperty("time_start")
    @JSONField(name = "time_start")
    var timeStart: String? = null

    /**
     * 交易结束时间
     */
    @JsonProperty("time_expire")
    @JSONField(name = "time_expire")
    var timeExpire: String? = null

    /**
     * 订单优惠标记
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
    var tradeType: TradeType = TradeType.JSAPI

    /**
     * 商品ID
     */
    @JsonProperty("product_id")
    @JSONField(name = "product_id")
    var productId: String? = null

    /**
     * 指定支付方式
     */
    @JsonProperty("limit_pay")
    @JSONField(name = "limit_pay")
    var limitPay: String? = null

    /**
     * 用户标识
     */
    var openid: String? = null

    /**
     * 电子发票入口开放标识
     */
    var receipt: String? = null

    /**
     * 是否需要分账
     */
    @JsonProperty("profit_sharing")
    @JSONField(name = "profit_sharing")
    var profitSharing: String? = null

    /**
     * 场景信息
     */
    @JsonProperty("scene_info")
    @JSONField(name = "scene_info")
    var sceneInfo: String? = null

    class SceneInfo {
        /**
         * 门店id
         */
        var id: String? = null

        /**
         * 门店名称
         */
        var name: String? = null

        /**
         * 门店行政区划码
         */
        @JsonProperty("area_code")
        @JSONField(name = "area_code")
        var areaCode: String? = null

        /**
         * 门店详细地址
         */
        var address: String? = null
    }
}