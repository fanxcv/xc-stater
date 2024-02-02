package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import lombok.Data

/**
 * @author fan
 */
@Data
class TemplateMessage {

    @Data
    class MiniProgram {
        /**
         * 所需跳转到的小程序appid（该小程序appid必须与发模板消息的公众号是绑定关联关系，暂不支持小游戏）
         */
        var appid: String? = null

        /**
         * 所需跳转到小程序的具体页面路径，支持带参数,（示例index?foo=bar），要求该小程序已发布，暂不支持小游戏
         */
        var pagepath: String? = null
    }

    /**
     * 模板数据
     */
    var data: MutableMap<String, MessageItem>? = null

    /**
     * 跳小程序所需数据，不需跳小程序可不用传该数据
     */
    var miniprogram: MiniProgram? = null

    /**
     * 模板ID
     */
    @JsonProperty("template_id")
    @JSONField(name = "template_id")
    var templateId: String? = null

    /**
     * 接收者openid
     */
    var touser: String? = null

    /**
     * 模板跳转链接（海外帐号没有跳转能力）
     */
    var url: String? = null
}
