package `fun`.fan.xc.plugin.weixin.entity

enum class RefundStatus(private val v: String) {
    /**
     * 退款成功
     */
    SUCCESS("SUCCESS"),

    /**
     * 退款异常
     */
    CHANGE("CHANGE"),

    /**
     * 退款关闭
     */
    REFUNDCLOSE("REFUNDCLOSE");

    override fun toString(): String = v
}