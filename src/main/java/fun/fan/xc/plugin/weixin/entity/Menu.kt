package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 *
 * {@see https://developers.weixin.qq.com/doc/offiaccount/Custom_Menus/Creating_Custom-Defined_Menu.html}
 * @author fan
 */
open class Menu {
    var button: MutableList<Button> = LinkedList<Button>()

    fun addButton(button: Button) {
        this.button.add(button)
    }

    class Button {
        var name: String? = null
        var type: String? = null
        var key: String? = null
        var url: String? = null

        @JsonProperty("media_id")
        @JSONField(name = "media_id")
        var mediaId: String? = null
        var appid: String? = null
        var pagepath: String? = null

        @JsonProperty("article_id")
        @JSONField(name = "article_id")
        var articleId: String? = null

        @JsonProperty("sub_button")
        @JSONField(name = "sub_button")
        var subButton: Button? = null
    }
}
