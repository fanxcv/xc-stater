package `fun`.fan.xc.starter.advice

import `fun`.fan.xc.starter.out.R
import jakarta.servlet.http.Cookie
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * @author fan
 */
@ControllerAdvice
class ResponseAdvice : ResponseBodyAdvice<Any> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        if (body is R<*>) {
            if (body.status != HttpStatus.OK) {
                response.setStatusCode(body.status)
            }
            if (response is ServletServerHttpResponse) {
                if (body.headers.isNotEmpty()) {
                    body.headers.forEach { (k, v) ->
                        response.servletResponse.addHeader(k, v)
                    }
                }

                if (body.cookies.isNotEmpty()) {
                    body.cookies.forEach { (k, v) ->
                        val cookie = Cookie(k, v)
                        cookie.path = "/"
                        response.servletResponse.addCookie(cookie)
                    }
                }
            }
            if (body.extendData.isNotEmpty()) {
                body.extendData["code"] = body.code
                body.extendData["body"] = body.body
                body.extendData["message"] = body.message
                return body.extendData
            }
            return body
        }

        // if (request is ServletServerHttpRequest) {
        //    val o = request.servletRequest.getAttribute(Dict.REQUEST_DEAL_BY_XC_CORE)
        //    if (o != null && o as Boolean) {
        //        return success(body)
        //    }
        // }

        return body
    }
}
