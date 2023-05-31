package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 公众号二维码创建返回信息
 * See https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html
 * @author fan
 */
open class QrCodeCreateResp {
    /**
     * 获取的二维码ticket，凭借此ticket可以在有效时间内换取二维码。
     */
    var ticket: String? = null

    /**
     * 该二维码有效时间，以秒为单位。 最大不超过2592000（即30天）
     */
    @JsonProperty("expire_seconds")
    @JSONField(name = "expire_seconds")
    var expireSeconds: Long? = null

    /**
     * 二维码图片解析后的地址，开发者可根据该地址自行生成需要的二维码图片
     */
    var url: String? = null

    /**
     * 拼接好, 可以直接请求的二维码地址
     */
    var wxUrl: String? = null
}