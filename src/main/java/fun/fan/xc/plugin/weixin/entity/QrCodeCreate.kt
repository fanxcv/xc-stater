package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 公众号二维码创建
 * See https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html
 * @author fan
 */
open class QrCodeCreate {
    /**
     * 该二维码有效时间，以秒为单位。 最大不超过2592000（即30天）
     */
    @JsonProperty("expire_seconds")
    @JSONField(name = "expire_seconds")
    var expireSeconds: Long? = 86400L

    /**
     * 二维码类型，QR_SCENE为临时的整型参数值，QR_STR_SCENE为临时的字符串参数值，
     */
    @JsonProperty("action_name")
    @JSONField(name = "action_name")
    var actionName: ActionNameEnum? = ActionNameEnum.QR_STR_SCENE

    /**
     * 二维码详细信息
     */
    @JsonProperty("action_info")
    @JSONField(name = "action_info")
    var actionInfo: ActionInfo? = null

    class ActionInfo(private var scene: Scene? = null)

    class Scene {
        /**
         * 场景值ID，临时二维码时为32位非0整型，永久二维码时最大值为100000（目前参数只支持1--100000）
         */
        @JsonProperty("scene_id")
        @JSONField(name = "scene_id")
        var sceneId: Long? = null

        /**
         * 场景值ID（字符串形式的ID），字符串类型，长度限制为1到64
         */
        @JsonProperty("scene_str")
        @JSONField(name = "scene_str")
        var sceneStr: String? = null

        constructor(sceneId: Long? = null, sceneStr: String? = null) {
            this.sceneId = sceneId
            this.sceneStr = sceneStr
        }

        constructor(sceneId: Long? = null) {
            this.sceneId = sceneId
        }

        constructor(sceneStr: String? = null) {
            this.sceneStr = sceneStr
        }
    }
}
