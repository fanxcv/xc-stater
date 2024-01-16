package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

class ProgramLoginResp : WXBaseResp() {
    /**
     * session_key
     */
    @JsonProperty("session_key")
    @JSONField(name = "session_key")
    var sessionKey: String? = null

    /**
     * 用户在开放平台的唯一标识符
     */
    var unionid: String? = null

    /**
     * 用户唯一标识
     */
    var openid: String? = null
}
