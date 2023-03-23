package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

class MediaUploadResp : WXBaseResp() {
    var type: String? = null

    @JsonProperty("media_id")
    @JSONField(name = "media_id")
    var mediaId: String? = null

    @JsonProperty("created_at")
    @JSONField(name = "created_at")
    var createdAt: Long? = null
}