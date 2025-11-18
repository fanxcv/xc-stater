package `fun`.fan.xc.plugin.proxy

import `fun`.fan.xc.plugin.proxy.config.ProxyConfigurationManager
import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import `fun`.fan.xc.plugin.proxy.interceptor.ProxyInterceptor
import `fun`.fan.xc.plugin.proxy.orchestrator.ProxyOrchestrator
import `fun`.fan.xc.plugin.proxy.transformer.HttpResponseTransformer
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockDataLoader
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockMatcher
import `fun`.fan.xc.plugin.proxy.mock.ProxyMockResponseHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Xc代理自动配置类
 *
 * @author fan
 */
@Configuration
@EnableConfigurationProperties(ProxyProperties::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class XcProxyAutoConfiguration(
    private val properties: ProxyProperties,
    private val orchestrator: ProxyOrchestrator,
    private val configManager: ProxyConfigurationManager,
    private val responseTransformer: HttpResponseTransformer,
    private val mockDataLoader: ProxyMockDataLoader,
    private val mockMatcher: ProxyMockMatcher,
    private val mockResponseHandler: ProxyMockResponseHandler
) : WebMvcConfigurer {

    private val log: Logger = LoggerFactory.getLogger(XcProxyAutoConfiguration::class.java)

    /**
     * 配置拦截器
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        val interceptor = registry
            .addInterceptor(ProxyInterceptor(properties, orchestrator, configManager, responseTransformer, mockDataLoader, mockMatcher, mockResponseHandler))
            .order(Ordered.HIGHEST_PRECEDENCE + 10)

        properties.route.forEach { interceptor.addPathPatterns(it.source) }

        // 统计启用Mock功能的路由数量
        val mockEnabledRoutes = properties.route.count { it.mock?.enabled == true }

        log.info("===> proxy: registering proxy interceptor, routes: {}, mock-enabled: {}",
                properties.route.map { it.source }, mockEnabledRoutes)
    }
}
