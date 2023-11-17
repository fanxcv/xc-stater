package `fun`.fan.xc.plugin.weixin.entity

enum class TradeType(private val v: String) {
    /**
     * JSAPI支付（或小程序支付）
     */
    JSAPI("JSAPI"),
    /**
     * 微信委托代扣支付
     */
    PAP("PAP");

    override fun toString(): String = v
}
