package `fun`.fan.xc.starter.handler

import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.out.R
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
import javax.servlet.http.HttpServletRequest
import javax.validation.ConstraintViolationException

@RestControllerAdvice
class XcGlobalExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(XcGlobalExceptionHandler::class.java)

    @Order(-1)
    @ExceptionHandler(value = [XcServiceException::class])
    fun xcServiceExceptionHandler(e: XcServiceException, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - 业务异常: ${e.code}: ${e.message}")
        return R.fail<Any>(e.code, e.message).status(e.status)
    }

    @Order(0)
    @ExceptionHandler(value = [XcRunException::class])
    fun xcRunExceptionHandler(e: XcRunException, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - ${e.code}: ${e.message}", e)
        return R.fail<Any>(e.code, e.message).status(e.status)
    }

    @Order(0)
    @ExceptionHandler(
        value = [
            ConstraintViolationException::class,
            MethodArgumentNotValidException::class,
            HttpMessageNotReadableException::class,
            MissingServletRequestParameterException::class,
            MissingPathVariableException::class
        ]
    )
    fun notValidExceptionHandler(e: Exception, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - 参数错误", e)
        val res = R.fail<Any>().status(HttpStatus.BAD_REQUEST)
        when (e) {
            is HttpMessageNotReadableException -> {
                res.message("请求体不能为空")
            }

            is MethodArgumentNotValidException -> {
                val errors = e.bindingResult.fieldErrors
                val messages = errors.map { "[${it.field}]: ${it.defaultMessage}" }
                res.message(messages.joinToString("\n"))
            }

            else -> {
                res.message(e.message)
            }
        }
        return res
    }

    @Order(0)
    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun illegalArgumentExceptionHandler(e: Exception, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - 业务异常: ${e.message}")
        return R.fail<Any>(ReturnCode.SYSTEM_ERROR).message(e.message)
    }

    @Order(9)
    @ExceptionHandler(value = [Exception::class])
    fun exceptionHandler(e: Exception, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - ${ReturnCode.SYSTEM_ERROR.code()}: ${e.message}", e)
        return R.fail<Any>(ReturnCode.SYSTEM_ERROR)
    }
}
