package `fun`.fan.xc.starter.exception

/**
 * @author fan
 */
class XcToolsException : XcRunException {
    constructor(msg: String?) : super(msg)
    constructor(cause: Throwable?) : super(cause)
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
}