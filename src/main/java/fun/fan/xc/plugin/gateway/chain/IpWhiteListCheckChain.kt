package `fun`.fan.xc.plugin.gateway.chain

import cn.hutool.cache.CacheUtil
import cn.hutool.core.text.AntPathMatcher
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import `fun`.fan.xc.plugin.gateway.ApiGroup
import `fun`.fan.xc.plugin.gateway.DefaultGatewayHandler.ApiCheckData
import `fun`.fan.xc.starter.XcConfiguration
import `fun`.fan.xc.starter.utils.Tools
import org.springframework.web.method.HandlerMethod
import java.util.*

/**
 * 通过Ip校验访问权限,白名单模式
 * 按url精确控制,组合
 */
open class IpWhiteListCheckChain(private val config: XcConfiguration) : AbstractIpCheckChain() {
    companion object {
        /**
         * 接口校验缓存
         */
        private val cache = CacheUtil.newLRUCache<String, Set<String>>(256)
        private val matcher = AntPathMatcher().setCachePatterns(true).setTrimTokens(true)
        private var configMap: Map<String, List<String>> = Maps.newHashMap()
        private var configCache: MutableList<XcConfiguration.IpCheck>? = null

        @JvmStatic
        fun cleanCache() = cache.clear()
    }

    private val default = "default"

    override fun doExec(v: ApiCheckData): Boolean? {
        val url = v.request.requestURI
        // 先从缓存获取相关的校验配置
        val checkList: Set<String> = cache[url] ?: initCheckList(v.handler, url)

        // 如果无法获取到任何规则匹配,返回校验失败
        if (checkList.isNotEmpty()) {
            if (v.ip == null) {
                v.ip = computedIp(v.request)
            }
            return Tools.ipIsInCidr(v.ip, checkList)
        }
        return false
    }

    /**
     * 计算URL的规则
     */
    @Synchronized
    private fun initCheckList(handler: HandlerMethod, url: String): Set<String> {
        log.debug("init {} check list", handler.toString())
        // 每次都去配置文件对象中获取,避免刷新后获取到历史配置
        val whiteList = getConfigMap()
        // 1. 默认需要添加DEFAULT的权限
        val list: MutableSet<String> = Sets.newHashSet()
        Optional.ofNullable(whiteList[default]).ifPresent { list.addAll(it) }
        // 2. 通过请求Url获取拦截规则
        for ((k, v) in whiteList) {
            if (matcher.match(k, url)) {
                list.addAll(v)
                break
            }
        }
        // 2.通过group查找拦截规则
        handler.getMethodAnnotation(ApiGroup::class.java)?.value
            ?.asList()?.stream()
            ?.flatMap { whiteList[it]?.stream() }
            ?.distinct()
            ?.forEach { if (it.isNotBlank()) list.add(it) }
        // 将规则缓存起来
        cache.put(url, list)

        return list
    }

    private fun getConfigMap(): Map<String, List<String>> {
        val whiteIps = config.gateway?.whiteIps
        if (configCache == whiteIps) {
            return configMap
        }

        val map = Maps.newHashMap<String, List<String>>()
        config.gateway?.whiteIps?.forEach {
            map[it.path] = it.ips
        }
        configMap = map
        configCache = whiteIps
        return configMap
    }
}
