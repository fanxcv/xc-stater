package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.plugin.weixin.enums.LangEnum
import `fun`.fan.xc.plugin.weixin.enums.MiniProgramStateEnum

class SubscribeMessage {

    /**
     * 模板ID
     */
    @JsonProperty("template_id")
    @JSONField(name = "template_id")
    var templateId: String = ""

    /**
     * 点击模板卡片后的跳转页面，仅限本小程序内的页面。支持带参数,（示例index?foo=bar）。该字段不填则模板无跳转
     */
    var page: String? = null

    /**
     * 接收者openid
     */
    var touser: String = ""

    /**
     * 模板数据
     */
    var data: MutableMap<String, MessageItem>? = null

    /**
     * 跳转小程序类型
     */
    @JsonProperty("miniprogram_state")
    @JSONField(name = "miniprogram_state")
    var miniProgramState: MiniProgramStateEnum = MiniProgramStateEnum.FORMAL

    /**
     * 进入小程序查看的语言类型
     */
    var lang: LangEnum = LangEnum.ZH_CN
}