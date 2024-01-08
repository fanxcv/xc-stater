package `fun`.fan.xc.plugin.gateway

import `fun`.fan.xc.plugin.gateway.chain.AbstractGatewayChain
import `fun`.fan.xc.plugin.gateway.chain.AbstractGatewayChain.Companion.builder
import `fun`.fan.xc.plugin.gateway.chain.IpBlackListCheckChain
import `fun`.fan.xc.plugin.gateway.chain.IpWhiteListCheckChain
import `fun`.fan.xc.starter.XcCoreAutoConfiguration
import jakarta.servlet.http.HttpServletRequest
import lombok.AllArgsConstructor
import lombok.Data
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.lang.NonNull
import org.springframework.util.Assert
import org.springframework.web.method.HandlerMethod

/**
 * 默认网关
 * IP黑名单校验 -> 限流 -> 请求时间间隔(依赖redis) -> 是否校验权限 -> 通过Token鉴权(依赖redis实现) -> IP白名单校验
 *
 * @author fan
 */
@Import(
    IpBlackListCheckChain::class,
    IpWhiteListCheckChain::class
)
@Configuration
@AutoConfigureAfter(XcCoreAutoConfiguration::class)
@ConditionalOnBean(XcCoreAutoConfiguration::class)
class DefaultGatewayHandler : XcGatewayHandler, BeanFactoryAware, InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private var gatewayChain: AbstractGatewayChain<ApiCheckData>? = null
    private var beanFactory: BeanFactory? = null

    override fun check(handler: HandlerMethod, request: HttpServletRequest): Boolean {
        val check = handler.getMethodAnnotation(ApiCheck::class.java)
        val data = ApiCheckData(request, handler, check, null)

        val res: Boolean? = gatewayChain?.exec(data)
        return res == null || res
    }

    override fun setBeanFactory(@NonNull beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    override fun afterPropertiesSet() {
        Assert.notNull(beanFactory, "beanFactory is must not be null")
        gatewayChain = builder<ApiCheckData>()
            .addChain(beanFactory!!.getBean(IpBlackListCheckChain::class.java))
            .addChain(beanFactory!!.getBean(IpWhiteListCheckChain::class.java))
            .build()
        log.info("===> init default xc gateway chain: {}", gatewayChain?.chain())
    }

    @Data
    @AllArgsConstructor
    data class ApiCheckData(
        val request: HttpServletRequest,
        val handler: HandlerMethod,
        val check: ApiCheck? = null,
        var ip: String? = null
    )
}
