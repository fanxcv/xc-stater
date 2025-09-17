package `fun`.fan.xc.plugin.proxy.exception

import `fun`.fan.xc.starter.exception.XcRunException

/**
 * 代理相关的业务异常
 *
 * @author fan
 */
class ProxyException : XcRunException {
    constructor(msg: String?) : super(msg)
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)
}
