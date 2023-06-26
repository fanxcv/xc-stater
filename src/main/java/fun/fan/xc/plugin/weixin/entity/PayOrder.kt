package `fun`.fan.xc.plugin.weixin.entity

import cn.hutool.core.date.DateUtil
import cn.hutool.core.lang.UUID
import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 小程序支付对象, 可直接在前端唤起支付
 * See https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=9_1&index=1
 * @author fan
 */
open class PayOrder {
    /**
     * 小程序ID
     */
    var appId: String? = null

    /**
     * 时间戳
     */
    var timeStamp: String = DateUtil.currentSeconds().toString()

    /**
     * 随机字符串
     */
    var nonceStr: String = UUID.fastUUID().toString(true)

    /**
     * 订单详情扩展字符串
     */
    @JsonProperty("package")
    @JSONField(name = "package")
    var packageValue: String? = null

    /**
     * 签名类型
     */
    var signType: SignType = SignType.MD5

    /**
     * 签名
     */
    var paySign: String? = null
}