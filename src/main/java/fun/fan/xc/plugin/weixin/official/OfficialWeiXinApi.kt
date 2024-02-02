package `fun`.fan.xc.plugin.weixin.official

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import `fun`.fan.xc.plugin.weixin.BaseWeiXinApi
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.entity.*
import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.utils.Dict
import `fun`.fan.xc.starter.utils.EncryptUtils
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Lazy
@Component
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialWeiXinApi(
    private val config: WeiXinConfig,
    private val accessTokenManager: OfficialAccessTokenManager,
    private val jsApiTicketManager: OfficialJsApiTicketManager
) : BaseWeiXinApi(accessTokenManager) {
    init {
        appId = config.official.appId
        appSecret = config.official.appSecret
    }

    /**
     * 微信服务器配置校验方法
     * 开发者通过检验signature对请求进行校验（下面有校验方式）。若确认此次GET请求来自微信服务器，请原样返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败。加密/校验流程如下：
     * 1）将token、timestamp、nonce三个参数进行字典序排序
     * 2）将三个参数字符串拼接成一个字符串进行sha1加密
     * 3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
     */
    fun checkSignature(nonce: String, timestamp: String, signature: String, echostr: String): String {
        val strings = arrayOf(nonce, timestamp, config.official.token)
        strings.sort()
        val signStr = "${strings[0]}${strings[1]}${strings[2]}"
        val sign = EncryptUtils.sha1(signStr)
        return if (sign == signature) echostr else Dict.BLANK
    }

    /**
     * JSTicket签名方法
     */
    fun jsTicketSign(noncestr: String, timestamp: Long, url: String): String {
        val ticket = jsApiTicketManager.token()
        val signStr = "jsapi_ticket=${ticket}&noncestr=${noncestr}&timestamp=${timestamp}&url=${url}"
        return EncryptUtils.sha1(signStr)
    }

    /**
     * 获取微信AccessToken
     */
    fun getJsTicket(): String = jsApiTicketManager.token()

    /**
     * 微信获取AccessToken && 基础登录
     * {
     * "access_token": "ACCESS_TOKEN",
     * "expires_in": 7200,
     * "refresh_token": "REFRESH_TOKEN",
     * "openid": "OPENID",
     * "scope": "SCOPE",
     * }
     */
    fun webLoginBase(code: String): WXLoginResp {
        // 2,通过code换取access_token
        return NetUtils.build(WeiXinDict.WX_API_LOGIN_BASE.format(appId, appSecret, code))
            .doGet { it ->
                JSON.parseObject(
                    it,
                    WXLoginResp::class.java,
                    JSONReader.Feature.UseBigDecimalForDoubles
                )
            }
    }

    /**
     * 微信获取用户个人信息
     * {
     * "openid": "OPENID",
     * "nickname": "NICKNAME",
     * "sex": 1,
     * "province": "PROVINCE",
     * "city": "CITY",
     * "country": "COUNTRY",
     * "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     * "privilege": ["PRIVILEGE1", "PRIVILEGE2"],
     * "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    fun webLoginUserInfo(code: String): WXUserInfo {
        val resp = webLoginBase(code)

        if (resp.accessToken == null) {
            val wxUserInfo = WXUserInfo()
            wxUserInfo.errcode = resp.errcode ?: -111111
            wxUserInfo.errmsg = resp.errmsg ?: "获取access token失败"
            return wxUserInfo
        }

        return NetUtils.build(WeiXinDict.WX_API_USER_INFO.format(resp.accessToken, resp.openid))
            .doGet { it ->
                JSON.parseObject(
                    it,
                    WXUserInfo::class.java,
                    JSONReader.Feature.UseBigDecimalForDoubles
                )
            }
    }

    /**
     * 客服接口-发消息
     * 文本
     * 图片
     */
    fun messageCustomSend(message: CustomMessage): Boolean {
        return NetUtils.build(WeiXinDict.WX_API_MESSAGE_CUSTOM.format(accessTokenManager.token()))
            .body(message)
            .doPost { it ->
                val bytes = it.readBytes()
                val resp: WXBaseResp =
                    JSON.parseObject(bytes, WXBaseResp::class.java, JSONReader.Feature.UseBigDecimalForDoubles)
                if (resp.errcode != null && resp.errcode != 0) {
                    log.debug("custom send result: {}", String(bytes, StandardCharsets.UTF_8))
                    throw XcServiceException(resp.errcode ?: -999999, "${resp.errcode}: ${resp.errmsg} \nmessage: $message")
                } else {
                    true
                }
            }
    }

    /**
     * 发送模板消息
     */
    fun messageTemplateSend(message: TemplateMessage): Boolean {
        return NetUtils.build(WeiXinDict.WX_API_MESSAGE_TEMPLATE.format(accessTokenManager.token()))
            .body(message)
            .doPost { it ->
                val bytes = it.readBytes()
                val resp: TemplateResp =
                    JSON.parseObject(bytes, TemplateResp::class.java, JSONReader.Feature.UseBigDecimalForDoubles)
                if (resp.errcode != null && resp.errcode != 0) {
                    log.debug("template send result: {}", String(bytes, StandardCharsets.UTF_8))
                    throw XcServiceException(resp.errcode ?: -999999, "${resp.errcode}: ${resp.errmsg} \nmessage: $message")
                } else {
                    true
                }
            }
    }

    /**
     * 微信临时素材上传
     */
    fun mediaUpload(file: File, type: WXType): MediaUploadResp {
        return FileInputStream(file).use {
            mediaUpload(it, file.name, type)
        }
    }

    /**
     * 微信临时素材上传
     */
    fun mediaUpload(`is`: InputStream, filename: String, type: WXType): MediaUploadResp {
        return NetUtils.build(WeiXinDict.WX_API_MEDIA_UPLOAD.format(accessTokenManager.token(), type))
            .addFile(filename, `is`)
            .doPost { it ->
                val bytes = it.readBytes()
                val resp: MediaUploadResp =
                    JSON.parseObject(bytes, MediaUploadResp::class.java, JSONReader.Feature.UseBigDecimalForDoubles)
                if (resp.errcode != null && resp.errcode != 0) {
                    log.debug("media upload result: {}", String(bytes, StandardCharsets.UTF_8))
                    throw XcServiceException(resp.errcode ?: -999999, "${resp.errcode}: ${resp.errmsg}")
                } else {
                    resp
                }
            }
    }

    /**
     * 生成带参数的二维码
     */
    fun createQrCode(param: QrCodeCreate): QrCodeCreateResp {
        val resp: QrCodeCreateResp = NetUtils.build(WeiXinDict.WX_CREATE_QR_CODE.format(accessTokenManager.token()))
            .body(param)
            .doPost(QrCodeCreateResp::class.java)
        resp.wxUrl = WeiXinDict.WX_QR_CODE_SHOW.format(resp.ticket)
        return resp
    }

    /**
     * 创建微信公众号菜单
     */
    fun createMenu(menu: Menu): WXBaseResp {
        return NetUtils.build(WeiXinDict.WX_MENU_CREATE.format(accessTokenManager.token()))
            .body(menu)
            .doPost(WXBaseResp::class.java)
    }
}
