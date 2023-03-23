package `fun`.fan.xc.starter.enums

import org.springframework.http.HttpStatus

/**
 * @author fan
 * @date Create in 10:53 2019-05-08
 */
enum class ReturnCode(
    /**
     * 错误码
     */
    private val code: Int,
    /**
     * HTTP 状态
     */
    private val status: HttpStatus,
    /**
     * 错误信息
     */
    private val message: String
) {
    SUCCESS(0, HttpStatus.OK, "success"),// 成功
    FAIL(-1, HttpStatus.INTERNAL_SERVER_ERROR, "failed"),// 失败 默认
    PARAM_ERROR(-2, HttpStatus.BAD_REQUEST, "Parameter error"),// 参数错误
    FORBIDDEN(-8, HttpStatus.FORBIDDEN, "Permission denied"),// 用户权限不足, 禁止访问
    UNAUTHORIZED(-9, HttpStatus.UNAUTHORIZED, "Unauthorized"),// 用户未认证
    SYSTEM_ERROR(9, HttpStatus.INTERNAL_SERVER_ERROR, "System error");// 系统错误

    fun message() = message

    fun status() = status

    fun code() = code
}