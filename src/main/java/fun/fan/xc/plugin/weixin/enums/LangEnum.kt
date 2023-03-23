package `fun`.fan.xc.plugin.weixin.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class LangEnum(
    @JsonValue
    val value: String
) {
    /**
     * 中文
     */
    ZH_CN("zh_CN"),

    /**
     * 英文
     */
    EN_US("en_US"),

    /**
     * 繁体中文
     */
    ZH_HK("zh_HK"),

    /**
     * 繁体中文
     */
    ZH_TW("zh_TW");

    companion object {
        @JsonCreator
        @JvmStatic
        fun getLang(value: String): LangEnum? {
            for (v in values()) {
                if (v.value == value) {
                    return v
                }
            }
            return null
        }
    }
}