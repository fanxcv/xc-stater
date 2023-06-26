package `fun`.fan.xc.plugin.weixin.entity

import com.alibaba.fastjson2.annotation.JSONField
import com.fasterxml.jackson.annotation.JsonProperty

open class PayBaseResp {
    /**
     * 返回状态码
     */
    @JsonProperty("return_code")
    @JSONField(name = "return_code")
    var returnCode: String? = null

    /**
     * 返回信息
     */
    @JsonProperty("return_msg")
    @JSONField(name = "return_msg")
    var returnMsg: String? = null

    /**
     * 业务结果
     */
    @JsonProperty("result_code")
    @JSONField(name = "result_code")
    var resultCode: String? = null

    /**
     * 错误代码
     */
    @JsonProperty("err_code")
    @JSONField(name = "err_code")
    var errCode: String? = null

    /**
     * 错误代码描述
     */
    @JsonProperty("err_code_des")
    @JSONField(name = "err_code_des")
    var errCodeDes: String? = null

    /**
     * 小程序ID
     */
    var appid: String? = null

    /**
     * 商户号
     */
    @JsonProperty("mch_id")
    @JSONField(name = "mch_id")
    var mchId: String? = null

    /**
     * 随机字符串
     */
    @JsonProperty("nonce_str")
    @JSONField(name = "nonce_str")
    var nonceStr: String? = null

    /**
     * 签名
     */
    var sign: String? = null
}