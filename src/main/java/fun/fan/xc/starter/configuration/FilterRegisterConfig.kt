package `fun`.fan.xc.starter.configuration

import `fun`.fan.xc.starter.filter.RequestWrapperFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * @author fan
 */
@Configuration
open class FilterRegisterConfig {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    open fun filterRegistrationBean(): FilterRegistrationBean<*> {
        log.info("===> core: register xc request wrapper filter")
        val filter = RequestWrapperFilter()
        val registrationBean = FilterRegistrationBean(filter)
        registrationBean.addUrlPatterns("/*")
        return registrationBean
    }
}
