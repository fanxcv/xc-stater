package `fun`.fan.xc.plugin.gateway.chain

import `fun`.fan.xc.plugin.gateway.DefaultGatewayHandler.ApiCheckData
import `fun`.fan.xc.starter.XcConfiguration
import `fun`.fan.xc.starter.utils.Tools

/**
 * 通过Ip校验访问权限,黑名单模式
 */
open class IpBlackListCheckChain(private val config: XcConfiguration) : AbstractIpCheckChain() {

    override fun doExec(v: ApiCheckData): Boolean? {
        val blackList = config.gateway?.blackIps
        // 如果有黑名单配置才进行黑名单校验
        if (blackList?.isNotEmpty() == true) {
            // 判断下Ip是不是已经计算过了
            if (v.ip == null) {
                v.ip = computedIp(v.request)
            }
            // 如果IP在黑名单列表里, 返回失败, 否则继续下步校验
            if (Tools.ipIsInCidr(v.ip, blackList)) {
                return false
            }
        }
        return null
    }
}
