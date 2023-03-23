package `fun`.fan.xc.plugin.weixin.official

import `fun`.fan.xc.plugin.weixin.WeiXinApiClient
import `fun`.fan.xc.plugin.weixin.WeiXinConfig
import `fun`.fan.xc.plugin.weixin.WeiXinInterceptor
import `fun`.fan.xc.plugin.weixin.WeixinClientEnable
import `fun`.fan.xc.starter.utils.NetUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.util.Assert

@Lazy
@Component
@ConditionalOnBean(WeixinClientEnable::class)
@ConditionalOnProperty(prefix = "xc.weixin.official", value = ["enable"], havingValue = "true", matchIfMissing = false)
class OfficialWeiXinApiClient(private val config: WeiXinConfig) : WeiXinApiClient, InitializingBean {
    private val c = config.client

    override fun accessToken(): String = NetUtils.build("${config.client.server}/official/accessToken")
        .addHeader(WeiXinInterceptor.APP_SECRET, c.auth.appSecret)
        .addHeader(WeiXinInterceptor.APP_ID, c.auth.appId)
        .doGet()

    override fun jsTicket(): String = NetUtils.build("${config.client.server}/official/jsTicket")
        .addHeader(WeiXinInterceptor.APP_SECRET, c.auth.appSecret)
        .addHeader(WeiXinInterceptor.APP_ID, c.auth.appId)
        .doGet()

    override fun afterPropertiesSet() {
        Assert.isTrue(StringUtils.isNotBlank(c?.server), "启用微信客户端模式后, 必须设置xc.weixin.client.server")
    }
}