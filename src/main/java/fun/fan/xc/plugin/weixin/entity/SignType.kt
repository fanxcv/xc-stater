package `fun`.fan.xc.plugin.weixin.entity

enum class SignType(private val v: String) {
    /**
     * MD5
     */
    MD5("MD5"),

    /**
     * HMAC-SHA256
     */
    HMAC_SHA256("HMAC-SHA256");

    override fun toString(): String = v
}