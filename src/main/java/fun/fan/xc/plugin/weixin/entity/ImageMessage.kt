package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author fan
 */
class ImageMessage : CustomMessage() {
    class Image {
        constructor()
        constructor(content: String?) {
            this.mediaId = content
        }

        @JsonProperty("media_id")
        @JSONField(name = "media_id")
        var mediaId: String? = null
    }

    /**
     * 图片消息内容
     */
    var image: Image? = null
}