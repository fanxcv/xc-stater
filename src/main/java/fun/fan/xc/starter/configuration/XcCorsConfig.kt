package `fun`.fan.xc.starter.configuration

import `fun`.fan.xc.starter.XcConfiguration
import `fun`.fan.xc.starter.XcConfiguration.CorsConfig
import org.apache.catalina.Context
import org.apache.tomcat.util.http.Rfc6265CookieProcessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * @author fan
 */
@Order
@Configuration
open class XcCorsConfig(private val xcConfig: XcConfiguration) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    @ConditionalOnMissingBean(CorsFilter::class)
    open fun corsFilter(): CorsFilter {
        log.info("===> core: init xc cors filter")
        val config: CorsConfig = xcConfig.cors
        log.info("origins allowed {}", config.allowedOrigins)
        log.info("headers allowed {}", config.allowedHeaders)
        log.info("methods allowed {}", config.allowedMethods)
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowedOriginPatterns = config.allowedOrigins
        corsConfiguration.allowedHeaders = config.allowedHeaders
        corsConfiguration.allowedMethods = config.allowedMethods
        // 接受cookie
        corsConfiguration.allowCredentials = true
        corsConfiguration.maxAge = 3600L
        val source = UrlBasedCorsConfigurationSource()
        config.path.forEach { source.registerCorsConfiguration(it, corsConfiguration) }
        return CorsFilter(source)
    }

    @Bean
    @ConditionalOnMissingBean(WebServerFactoryCustomizer::class)
    open fun cookieProcessorCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        log.info("===> core: init xc cookie processor customizer")
        return WebServerFactoryCustomizer { factory: TomcatServletWebServerFactory ->
            factory.addContextCustomizers(
                TomcatContextCustomizer { context: Context -> context.cookieProcessor = Rfc6265CookieProcessor() })
        }
    }
}
