package `fun`.fan.xc.starter.exception

import `fun`.fan.xc.starter.enums.ReturnCode
import org.springframework.http.HttpStatus

/**
 * @author fan
 */
open class XcRunException : RuntimeException {
    var status = HttpStatus.INTERNAL_SERVER_ERROR
        private set
    var code = ReturnCode.FAIL.code()
        private set

    constructor(rc: ReturnCode) : super(rc.message()) {
        this.status = rc.status()
        this.code = rc.code()
    }

    constructor(rc: ReturnCode, msg: String?) : super(msg) {
        this.status = rc.status()
        this.code = rc.code()
    }

    constructor(code: Int, msg: String?) : super(msg) {
        this.code = code
    }

    constructor(msg: String?) : super(msg)
    constructor(cause: Throwable?) : super(cause)
    constructor(code: Int, cause: Throwable?) : super(cause) {
        this.code = code
    }

    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
    constructor(code: Int, msg: String?, cause: Throwable?) : super(msg, cause) {
        this.code = code
    }
}
