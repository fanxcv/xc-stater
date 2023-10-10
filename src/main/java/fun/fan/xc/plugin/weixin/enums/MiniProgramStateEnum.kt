package `fun`.fan.xc.plugin.weixin.enums

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class MiniProgramStateEnum(
    @JsonValue
    @JSONField
    val value: String
) {
    /**
     * 开发版
     */
    DEVELOPER("developer"),

    /**
     * 体验版
     */
    TRIAL("trial"),

    /**
     * 正式版
     */
    FORMAL("formal");

    companion object {
        @JsonCreator
        @JvmStatic
        fun getState(value: String): MiniProgramStateEnum? {
            for (v in MiniProgramStateEnum.values()) {
                if (v.value == value) {
                    return v
                }
            }
            return null
        }
    }
}
