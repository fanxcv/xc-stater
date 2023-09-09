package `fun`.fan.xc.starter.exception

import `fun`.fan.xc.starter.enums.ReturnCode
import org.springframework.http.HttpStatus

/**
 * @author fan
 */
open class XcServiceException : XcRunException {
    constructor(rc: ReturnCode) : super(rc)

    constructor(code: Int, msg: String?) : super(code, msg)

    constructor(msg: String?) : super(msg)
    constructor(cause: Throwable?) : super(cause)
    constructor(code: Int, cause: Throwable?) : super(code, cause)

    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
    constructor(code: Int, msg: String?, cause: Throwable?) : super(code, msg, cause)
}
