package `fun`.fan.xc.plugin.weixin

import com.google.common.collect.Sets
import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.exception.XcRunException
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@ConditionalOnBean(WeixinServerEnable::class)
class WeiXinInterceptor(private val config: WeiXinConfig) : HandlerInterceptor, WebMvcConfigurer, InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val cache: MutableSet<String> = Sets.newHashSet()

    companion object {
        const val APP_ID = "App-Id"
        const val APP_SECRET = "App-Secret"
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val appId = request.getHeader(APP_ID)
        val appSecret = request.getHeader(APP_SECRET)
        return if (StringUtils.isNoneBlank(appId, appSecret) && cache.contains("$appId:$appSecret")) {
            true
        } else {
            throw XcRunException(ReturnCode.FORBIDDEN)
        }
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (config.server?.isEnableAuth == true) {
            log.info("===> weixin: register weixin interceptors")
            registry.addInterceptor(this)
                .excludePathPatterns("${config.server.basePath}/official/signature")
                .addPathPatterns("${config.server.basePath}/**")
        }
    }

    override fun afterPropertiesSet() {
        config.server.auth.forEach { cache.add("${it.appId}:${it.appSecret}") }
    }
}