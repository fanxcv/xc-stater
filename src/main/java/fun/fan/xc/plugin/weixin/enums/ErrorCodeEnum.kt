package `fun`.fan.xc.plugin.weixin.enums

import com.fasterxml.jackson.annotation.JsonValue

enum class ErrorCodeEnum(
    @JsonValue
    val value: Int,
    @JsonValue
    val message: String
) {
    OK(0, "请求成功"),
    INVALID_APPID(40013, "不合法的 AppID ，请开发者检查 AppID 的正确性，避免异常字符，注意大小写")
}