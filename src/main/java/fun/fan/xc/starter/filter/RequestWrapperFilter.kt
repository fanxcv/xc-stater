package `fun`.fan.xc.starter.filter

import `fun`.fan.xc.starter.utils.Dict
import `fun`.fan.xc.starter.wrapper.BufferedServletRequestWrapper
import lombok.extern.slf4j.Slf4j
import org.springframework.http.MediaType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest


@Slf4j
@WebFilter(urlPatterns = ["#{ xcConfiguration.core.path }"], filterName = "verificationFilter")
class RequestWrapperFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request !is HttpServletRequest) {
            return chain.doFilter(request, response)
        }
        val contentType: String? = request.contentType
        if (contentType?.contains(MediaType.APPLICATION_JSON_VALUE) == true) {
            request.setAttribute(Dict.REQUEST_USE_WRAPPER, true)
            val wrapper = BufferedServletRequestWrapper(request)
            return chain.doFilter(wrapper, response)
        }
        chain.doFilter(request, response)
    }
}
