package `fun`.fan.xc.starter.exception

import `fun`.fan.xc.starter.enums.ReturnCode

/**
 * @author fan
 */
class ParamErrorException : XcRunException {
    constructor(msg: String?) : super(ReturnCode.PARAM_ERROR.code(), msg)
    constructor(msg: String?, cause: Throwable?) : super(ReturnCode.PARAM_ERROR.code(), msg, cause)
}