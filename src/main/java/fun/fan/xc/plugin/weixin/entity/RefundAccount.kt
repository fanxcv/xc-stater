package `fun`.fan.xc.plugin.weixin.entity

enum class RefundAccount(private val v: String) {
    /**
     * 未结算资金退款
     */
    REFUND_SOURCE_UNSETTLED_FUNDS("REFUND_SOURCE_UNSETTLED_FUNDS"),

    /**
     * 可用余额退款
     */
    REFUND_SOURCE_RECHARGE_FUNDS("REFUND_SOURCE_RECHARGE_FUNDS");

    override fun toString(): String = v
}