package `fun`.fan.xc.plugin.proxy.exception

import `fun`.fan.xc.starter.exception.XcRunException
import java.util.concurrent.TimeoutException

/**
 * 代理相关的业务异常
 *
 * @author fan
 */
class ProxyException : XcRunException {
    constructor(msg: String?) : super(msg)
}

/**
 * 连接池超时异常
 * 用于标识连接池相关的超时错误，便于负载均衡器识别和处理
 *
 * @author fan
 */
class ConnectionPoolTimeoutException : TimeoutException {
    constructor(msg: String?) : super(msg)
}
