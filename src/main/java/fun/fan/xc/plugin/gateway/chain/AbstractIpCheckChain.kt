package `fun`.fan.xc.plugin.gateway.chain

import `fun`.fan.xc.plugin.gateway.DefaultGatewayHandler
import `fun`.fan.xc.starter.utils.Dict
import `fun`.fan.xc.starter.utils.Tools
import javax.servlet.http.HttpServletRequest


/**
 * IP校验父类, 提供获取应用IP能力
 */
abstract class AbstractIpCheckChain : AbstractGatewayChain<DefaultGatewayHandler.ApiCheckData>() {

    /**
     * 计算请求IP
     * 从右侧开始, 获取第一个非内网IP, 如果全部都是内网IP, 则保留最后一个IP, 即起始第一个IP
     */
    protected fun computedIp(request: HttpServletRequest): String {
        val ip = Tools.getIpAddress(request)
        val ips: List<String> = ip?.split(",") ?: listOf()
        return when {
            ips.size == 1 -> ips[0]
            ips.size > 1 -> {
                for (i in ips.size downTo 1) {
                    if (!Tools.isInnerIp(ips[i])) {
                        return ips[i]
                    }
                }
                return ips[0]
            }

            else -> Dict.BLANK
        }
    }
}
