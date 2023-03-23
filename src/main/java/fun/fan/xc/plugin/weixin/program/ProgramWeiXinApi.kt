package `fun`.fan.xc.plugin.weixin.program

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import `fun`.fan.xc.plugin.weixin.BaseWeiXinApi
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinDict
import `fun`.fan.xc.plugin.weixin.entity.ProgramLoginResp
import `fun`.fan.xc.plugin.weixin.entity.SubscribeMessage
import `fun`.fan.xc.plugin.weixin.entity.WXBaseResp
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

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
}