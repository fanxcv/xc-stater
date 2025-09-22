package `fun`.fan.xc.starter.resolver

import com.alibaba.fastjson2.JSON
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.out.R
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import java.lang.Exception
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class XcHandlerExceptionResolver() : HandlerExceptionResolver {
    private val log = LoggerFactory.getLogger(XcHandlerExceptionResolver::class.java)

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception
    ): ModelAndView? {
        if (ex is XcRunException) {
            log.error("${request.requestURI} - Resolver异常: ${ex.code}: ${ex.message}")

            response.status = ex.status.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = StandardCharsets.UTF_8.name()
            val fail = R.fail<Void>(ex.code, ex.message)
            response.writer.write(JSON.toJSONString(fail))
            response.writer.flush()

            return ModelAndView()
        }

        return null
    }
}
