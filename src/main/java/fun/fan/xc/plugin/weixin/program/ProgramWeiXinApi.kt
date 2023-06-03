package `fun`.fan.xc.plugin.weixin.program

import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.plugin.weixin.BaseWeiXinApi
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.WeiXinUtils
import `fun`.fan.xc.plugin.weixin.entity.*
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.utils.BeanUtils
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Lazy
@Component
@ConditionalOnProperty(
    prefix = "xc.weixin.mini-program",
    value = ["enable"],
    havingValue = "true",
    matchIfMissing = false
)
class ProgramWeiXinApi(
    private val config: WeiXinConfig,
    private val accessTokenManager: ProgramAccessTokenManager
) : BaseWeiXinApi(config, accessTokenManager) {
    init {
        appId = config.miniProgram.appId
        appSecret = config.miniProgram.appSecret
    }

    /**
     * 小程序登录
     */
    fun code2Session(code: String): ProgramLoginResp {
        return NetUtils.build()
            .url(WeiXinDict.WX_API_CODE_TO_SESSION.format(appId, appSecret, code))
            .doGet(ProgramLoginResp::class.java)
    }

    /**
     * 发送订阅消息
     */
    fun sendMessage(message: SubscribeMessage): Boolean {
        return NetUtils.build(WeiXinDict.WX_API_MESSAGE_SUBSCRIBE.format(accessTokenManager.token()))
            .body(message)
            .doPost { it ->
                val bytes = it.readBytes()
                val resp: WXBaseResp = JSON.parseObject(bytes, WXBaseResp::class.java)
                if (resp.errcode != null && resp.errcode != 0) {
                    log.debug("subscribe send result: {}", String(bytes, StandardCharsets.UTF_8))
                    throw XcRunException(resp.errcode ?: -999999, "${resp.errcode}: ${resp.errmsg}")
                } else {
                    true
                }
            }
    }

    /**
     * 小程序统一下单接口
     */
    fun payUnifiedOrder(order: PayUnifiedOrder): PayUnifiedOrderResp {
        if (order.sign.isNullOrBlank()) {
            val map = BeanUtils.beanToMap(order) {
                val method = it.readMethod
                val property = method.getAnnotation(JsonProperty::class.java)
                property.value
            }
            val sign = WeiXinUtils.sign(map, config.miniProgram.signKey)
            order.sign = sign
        }
        return NetUtils.build(WeiXinDict.WX_API_PAY_UNIFIED_ORDER)
            .contentType(MediaType.APPLICATION_XML)
            .body(order)
            .doPost { it ->
                val bytes = it.readBytes()
                NetUtils.XMLMapper.readValue(bytes, PayUnifiedOrderResp::class.java)
            }
    }

    /**
     * 小程序统一下单接口, 直接返回能在前端调起支付的对象 m m
     */
    fun payOrderSimple(order: PayUnifiedOrder): PayOrder {
        val resp: PayUnifiedOrderResp = payUnifiedOrder(order)

        if (resp.returnCode != "SUCCESS" || resp.resultCode != "SUCCESS") {
            throw XcRunException("微信支付统一下单失败: ${resp.returnMsg}")
        }

        val res = PayOrder()
        res.appId = order.appid
        res.packageValue = "prepay_id=${resp.prepayId}"
        val map = BeanUtils.beanToMap(res) {
            val method = it.readMethod
            val property = method.getAnnotation(JsonProperty::class.java)
            property.value
        }
        res.sign = WeiXinUtils.sign(map, config.miniProgram.signKey)

        return res
    }

    /**
     * 小程序支付回调
     */
    fun payNotify(request: HttpServletRequest, response: HttpServletResponse, action: (PayNotifyResp) -> Boolean) {
        request.inputStream.use {
            val resp = NetUtils.XMLMapper.readValue(it.readBytes(), PayNotifyResp::class.java)
            val res: Map<String, String> =
                if (action(resp)) {
                    mapOf("return_code" to "SUCCESS", "return_msg" to "OK")
                } else {
                    mapOf("return_code" to "FAIL", "return_msg" to "FAIL")
                }
            response.writer.write(NetUtils.XMLMapper.writeValueAsString(res))
        }
    }
}