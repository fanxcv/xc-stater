package `fun`.fan.xc.plugin.weixin.enums

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonValue

enum class MPVersionEnum(
    @JsonValue
    @JSONField
    val value: String
) {
    /**
     * 开发版
     */
    DEVELOP("develop"),

    /**
     * 体验版
     */
    TRIAL("trial"),

    /**
     * 正式版
     */
    RELEASE("release");
}
