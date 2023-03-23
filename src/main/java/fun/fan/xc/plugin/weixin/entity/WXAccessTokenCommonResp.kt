package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

class WXAccessTokenCommonResp : WXBaseResp() {

    @JsonProperty("access_token")
    @JSONField(name = "access_token")
    var accessToken: String? = null

    /**
     * token到期时间
     */
    @JsonProperty("expires_in")
    @JSONField(name = "expires_in")
    var expiresIn: Int? = null
}