package `fun`.fan.xc.plugin.weixin.entity

class MessageItem {
    var value: String? = null

    /**
     * 模板内容字体颜色，不填默认为黑色
     */
    var color: String? = null

    constructor()
    constructor(value: String) {
        this.value = value
    }

    constructor(value: String, color: String? = null) {
        this.value = value
        this.color = color
    }
}