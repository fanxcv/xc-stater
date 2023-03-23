package `fun`.fan.xc.plugin.weixin.entity

enum class WXType(private val v: String) {
    /**
     * 文本消息
     */
    TEXT("text"),

    /**
     * 图片消息
     */
    IMAGE("image");

    override fun toString(): String = v
}