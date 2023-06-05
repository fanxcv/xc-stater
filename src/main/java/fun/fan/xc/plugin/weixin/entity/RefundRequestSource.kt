package `fun`.fan.xc.plugin.weixin.entity

enum class RefundRequestSource(private val v: String) {
    /**
     * API接口
     */
    API("API"),

    /**
     * 商户平台
     */
    VENDOR_PLATFORM("VENDOR_PLATFORM");

    override fun toString(): String = v
}