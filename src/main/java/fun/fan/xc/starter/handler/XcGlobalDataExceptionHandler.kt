package `fun`.fan.xc.starter.handler

import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.out.R
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import jakarta.servlet.http.HttpServletRequest

@RestControllerAdvice
@ConditionalOnClass(DataAccessException::class)
class XcGlobalDataExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(XcGlobalDataExceptionHandler::class.java)

    @Order(0)
    @ExceptionHandler(value = [DataAccessException::class])
    fun sqlExceptionHandler(e: DataAccessException, request: HttpServletRequest): Any {
        log.error("${request.requestURI} - 数据库执行异常: ${e.message}")
        return R.fail<Any>(ReturnCode.DB_ERROR)
    }
}
