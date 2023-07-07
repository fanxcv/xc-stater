package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

class WXLoginResp : WXBaseResp() {
    /**
     * access_token
     */
    @JsonProperty("access_token")
    @JSONField(name = "access_token")
    var accessToken: String? = null

    /**
     * token到期时间
     */
    @JsonProperty("expires_in")
    @JSONField(name = "expires_in")
    var expiresIn: Int? = null

    /**
     * 用户刷新access_token
     */
    @JsonProperty("refresh_token")
    @JSONField(name = "refresh_token")
    var refreshToken: String? = null

    /**
     * 用户唯一标识
     */
    var openid: String? = null

    /**
     * 用户授权的作用域，使用逗号（,）分隔
     */
    var scope: String? = null

    /**
     * 是否为快照页模式虚拟账号，只有当用户是快照页模式虚拟账号时返回，值为1
     */
    @JsonProperty("is_snapshotuser")
    @JSONField(name = "is_snapshotuser")
    var snapshotUser: Int? = null

    /**
     * 用户在开放平台的唯一标识符
     */
    var unionid: String? = null
}