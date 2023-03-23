package `fun`.fan.xc.starter.exception

import `fun`.fan.xc.starter.enums.ReturnCode

/**
 * @author fan
 */
class BussinessException : XcRunException {
    constructor() : super(ReturnCode.FAIL)
    constructor(msg: String?) : super(msg)
    constructor(cause: Throwable?) : super(cause)
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
}