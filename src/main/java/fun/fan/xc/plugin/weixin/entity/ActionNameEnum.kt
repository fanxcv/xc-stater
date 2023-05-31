package `fun`.fan.xc.plugin.weixin.entity

enum class ActionNameEnum(private val v: String) {
    /**
     * 临时的整型参数值
     */
    QR_SCENE("QR_SCENE"),
    /**
     * 临时的字符串参数值
     */
    QR_STR_SCENE("QR_STR_SCENE"),
    /**
     * 永久的整型参数值
     */
    QR_LIMIT_SCENE("QR_LIMIT_SCENE"),
    /**
     * 永久的字符串参数值
     */
    QR_LIMIT_STR_SCENE("QR_LIMIT_STR_SCENE");

    override fun toString(): String = v
}