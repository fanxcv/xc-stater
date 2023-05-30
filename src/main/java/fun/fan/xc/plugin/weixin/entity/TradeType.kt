package `fun`.fan.xc.plugin.weixin.entity

enum class TradeType(private val v: String) {
    /**
     * JSAPI支付（或小程序支付）
     */
    JSAPI("JSAPI");

    override fun toString(): String = v
}