package `fun`.fan.xc.plugin.weixin.program

import cn.hutool.core.codec.Base64
import cn.hutool.core.util.StrUtil
import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.plugin.weixin.BaseWeiXinApi
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.WeiXinUtils
import `fun`.fan.xc.plugin.weixin.entity.*
import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.utils.BeanUtils
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
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
) : BaseWeiXinApi(accessTokenManager) {
    private val wxSslSocketFactory: SSLSocketFactory by lazy {
        val keyStore = KeyStore.getInstance("PKCS12")
        Assert.isTrue(StrUtil.isNotBlank(config.miniProgram.pay.apiCertPath), "微信支付证书路径不能为空")
        DefaultResourceLoader().getResource(config.miniProgram.pay.apiCertPath)
            .inputStream.use {
                keyStore.load(it, config.miniProgram.pay.mchId.toCharArray())
            }
        // 初始化KeyManagerFactory
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, config.miniProgram.pay.mchId.toCharArray())

        // 初始化SSLContext
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, null, null)
        sslContext.socketFactory
    }

    init {
        pay = config.miniProgram.pay
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
                    throw XcServiceException(resp.errcode ?: -999999, "${resp.errcode}: ${resp.errmsg}")
                } else {
                    true
                }
            }
    }

    /**
     * 小程序统一下单接口
     */
    fun payUnifiedOrder(order: PayUnifiedOrder): PayUnifiedOrderResp {
        WeiXinUtils.sign(order, config.miniProgram.pay.apiV2Key)
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

        if (resp.returnCode != WeiXinDict.WX_SUCCESS) {
            throw XcServiceException("微信支付统一下单失败: ${resp.returnMsg}")
        }

        if (resp.resultCode != WeiXinDict.WX_SUCCESS) {
            throw XcServiceException("微信支付统一下单失败: ${resp.errCode} ${resp.errCodeDes}")
        }

        val res = PayOrder()
        res.appId = order.appid
        res.packageValue = "prepay_id=${resp.prepayId}"
        val map = BeanUtils.beanToMap(res) {
            val property = it.getAnnotation(JsonProperty::class.java)
            property?.value ?: it.name
        }
        res.paySign = WeiXinUtils.sign(map, config.miniProgram.pay.apiV2Key)

        return res
    }

    /**
     * 小程序支付回调
     */
    fun payNotify(request: HttpServletRequest, response: HttpServletResponse, action: (PayNotifyResp) -> Boolean) {
        request.inputStream.use {
            val resp = NetUtils.XMLMapper.readValue(it.readBytes(), PayNotifyResp::class.java)
            doNotify(action(resp), response)
        }
    }

    /**
     * 退款接口
     */
    fun payRefund(fund: PayRefund): PayRefundResp {
        WeiXinUtils.sign(fund, config.miniProgram.pay.apiV2Key)
        return NetUtils.build(WeiXinDict.WX_API_PAY_REFUND)
            .contentType(MediaType.APPLICATION_XML)
            .body(fund)
            .beforeRequest {
                if (it.url.protocol != "https") {
                    throw XcServiceException("微信退款接口必须使用 https 协议")
                }
                (it as HttpsURLConnection).sslSocketFactory = wxSslSocketFactory
            }
            .doPost { it ->
                val bytes = it.readBytes()
                NetUtils.XMLMapper.readValue(bytes, PayRefundResp::class.java)
            }
    }

    /**
     * 小程序退款回调
     */
    fun payRefundNotify(
        request: HttpServletRequest,
        response: HttpServletResponse,
        action: (PayRefundNotifyResp) -> Boolean
    ) {
        request.inputStream.use {
            val resp = NetUtils.XMLMapper.readValue(it.readBytes(), PayRefundNotifyResp::class.java)
            resp.refundInfo = WeiXinUtils.decodeRefundInfo(resp.reqInfo!!, config.miniProgram.pay.apiV2Key)
            doNotify(action(resp), response)
        }
    }

    /**
     * 委托代扣-支付中签约
     * See https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_5.shtml
     */
    fun payContractOrder(order: PayContractOrder): PayContractOrderResp {
        WeiXinUtils.sign(order, config.miniProgram.pay.apiV2Key)
        return NetUtils.build(WeiXinDict.WX_API_PAY_CONTRACT_ORDER)
            .contentType(MediaType.APPLICATION_XML)
            .body(order)
            .doPost { it ->
                val bytes = it.readBytes()
                NetUtils.XMLMapper.readValue(bytes, PayContractOrderResp::class.java)
            }
    }

    /**
     * 委托代扣-申请扣款
     * see https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_8.shtml
     */
    fun payPapPayApply(apply: PayPapPayApply): PayBaseResp {
        WeiXinUtils.sign(apply, config.miniProgram.pay.apiV2Key)
        return NetUtils.build(WeiXinDict.WX_API_PAY_PAP_APPLY)
            .contentType(MediaType.APPLICATION_XML)
            .body(apply)
            .beforeRequest {
                if (it.url.protocol != "https") {
                    throw XcServiceException("微信退款接口必须使用 https 协议")
                }
                (it as HttpsURLConnection).sslSocketFactory = wxSslSocketFactory
            }
            .doPost { it ->
                val bytes = it.readBytes()
                NetUtils.XMLMapper.readValue(bytes, PayBaseResp::class.java)
            }
    }

    /**
     * 委托代扣-扣款结果通知
     * see https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter4_2.shtml
     */
    fun payPapPayApplyNotify(
        request: HttpServletRequest,
        response: HttpServletResponse,
        action: (PayPapPayApplyNotifyResp) -> Boolean
    ) {
        request.inputStream.use {
            val resp = NetUtils.XMLMapper.readValue(it.readBytes(), PayPapPayApplyNotifyResp::class.java)
            doNotify(action(resp), response)
        }
    }

    /**
     * 委托代扣-申请解约
     * see https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_9.shtml
     */
    fun payDeleteContract(v: PayDeleteContract): PayDeleteContractResp {
        WeiXinUtils.sign(v, config.miniProgram.pay.apiV2Key)
        return NetUtils.build(WeiXinDict.WX_API_PAY_DELETE_CONTRACT)
            .contentType(MediaType.APPLICATION_XML)
            .body(v)
            .beforeRequest {
                if (it.url.protocol != "https") {
                    throw XcServiceException("微信退款接口必须使用 https 协议")
                }
                (it as HttpsURLConnection).sslSocketFactory = wxSslSocketFactory
            }
            .doPost { it ->
                val bytes = it.readBytes()
                NetUtils.XMLMapper.readValue(bytes, PayDeleteContractResp::class.java)
            }
    }

    /**
     * 委托代扣-签约,解约结果通知
     * see https://pay.weixin.qq.com/wiki/doc/api/wxpay_v2/papay/chapter3_6.shtml
     */
    fun payAddOrDelContractNotify(
        request: HttpServletRequest,
        response: HttpServletResponse,
        action: (PayAddOrDelContractNotifyResp) -> Boolean
    ) {
        request.inputStream.use {
            val resp = NetUtils.XMLMapper.readValue(it.readBytes(), PayAddOrDelContractNotifyResp::class.java)
            doNotify(action(resp), response)
        }
    }

    /**
     * 获取不限制的小程序码
     */
    fun getUnlimitedQRCode(param: MPUnlimitedQRCode): String {
        return NetUtils.build(WeiXinDict.MP_UNLIMITED_QR_CODE.format(accessTokenManager.token()))
            .body(param)
            .doPost { `is` ->
                val available = `is`.available()
                if (available < 256) {
                    val msg = BufferedReader(InputStreamReader(`is`)).use(BufferedReader::readText)
                    throw XcServiceException("request QRCode failed, message: $msg")
                }
                Base64.encode(`is`)
            }
    }

    private fun doNotify(exec: Boolean, response: HttpServletResponse) {
        val res: Map<String, String> =
            if (exec) {
                mapOf("return_code" to "SUCCESS", "return_msg" to "OK")
            } else {
                mapOf("return_code" to "FAIL", "return_msg" to "FAIL")
            }
        response.writer.write(NetUtils.XMLMapper.writeValueAsString(res))
    }
}
