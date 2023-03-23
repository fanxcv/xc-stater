package `fun`.fan.xc.starter.handler

import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.out.R
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @Order(0)
    @ExceptionHandler(value = [XcRunException::class])
    fun xcRunExceptionHandler(e: XcRunException, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - ${e.code}: ${e.message}", e)
        return R.fail<Any>(e.code, e.message).setStatus(e.status)
    }

    @Order(0)
    @ExceptionHandler(
        value = [
            ConstraintViolationException::class,
            MethodArgumentNotValidException::class,
            HttpMessageNotReadableException::class,
            MissingServletRequestParameterException::class,
            MissingPathVariableException::class]
    )
    fun notValidExceptionHandler(e: Exception, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - 参数错误: ${e.message}", e)
        val res = R.fail<Any>().setStatus(HttpStatus.BAD_REQUEST)
        when (e) {
            is HttpMessageNotReadableException -> {
                res.setMsg("请求体不能为空")
            }

            else -> {
                res.setMsg(e.message)
            }
        }
        return res
    }

    @Order(9)
    @ExceptionHandler(value = [Exception::class])
    fun exceptionHandler(e: Exception, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - ${ReturnCode.SYSTEM_ERROR.code()}: ${e.message}", e)
        return R.fail<Any>(ReturnCode.SYSTEM_ERROR)
    }
}