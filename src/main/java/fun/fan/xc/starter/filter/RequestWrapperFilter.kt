package `fun`.fan.xc.starter.filter

import `fun`.fan.xc.starter.wrapper.BufferedServletRequestWrapper
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebFilter
import javax.servlet.http.HttpServletRequest


@Slf4j
@WebFilter(urlPatterns = ["#{ xcConfiguration.core.path }"], filterName = "verificationFilter")
class RequestWrapperFilter : Filter {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request !is HttpServletRequest) {
            return chain.doFilter(request, response)
        }
        val contentType: String? = request.contentType
        if (contentType?.contains(MediaType.APPLICATION_JSON_VALUE) == true) {
            log.debug("use request wrapper")
            val wrapper = BufferedServletRequestWrapper(request)
            return chain.doFilter(wrapper, response)
        }
        chain.doFilter(request, response)
    }
}