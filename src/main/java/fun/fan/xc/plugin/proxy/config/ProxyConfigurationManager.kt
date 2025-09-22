package `fun`.fan.xc.plugin.proxy.config

import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * 代理配置管理器
 * 专注于代理配置的管理和查询，不涉及具体的请求处理和负载均衡逻辑
 *
 * @author fan
 *
 * ## 职责范围
 * - 代理路由配置的初始化和管理
 * - 配置映射的缓存和查询
 * - 配置合法性验证
 * - 配置变更通知（如果需要）
 *
 * ## 不负责的功能
 * - HTTP请求处理（委托给ProxyOrchestrator）
 * - 负载均衡（委托给ProxyLoadBalancer）
 * - 连接管理（委托给HostPortChannelPool）
 *
 * ## 使用示例
 * ```kotlin
 * val configManager = ProxyConfigurationManager(proxyProperties)
 * val matchedConfig = configManager.findProxyConfig(requestPath)
 * ```
 */
class ProxyConfigurationManager(
    private val properties: ProxyProperties
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyConfigurationManager::class.java)

    // 路由映射缓存
    private val routeMap = mutableMapOf<String, ProxyConfigMatch>()

    init {
        initializeRouteMap()
    }

    /**
     * 初始化路由映射配置
     */
    private fun initializeRouteMap() {
        properties.route?.forEach { route ->
            validateRouteConfiguration(route)
            if (route.target != null && route.target.isNotEmpty()) {
                routeMap[route.source] = ProxyConfigMatch(route, route.target)
                // log.debug("Added route mapping: {} -> {}", route.source, route.target.map { it.uri })
            }
        }
        // log.info("Initialized {} proxy routes", routeMap.size)
        logRouteSummary()
    }

    /**
     * 验证路由配置的合法性
     */
    private fun validateRouteConfiguration(route: ProxyRoute) {
        if (route.source.isNullOrBlank()) {
            throw ProxyException("Route source path cannot be null or blank")
        }

        if (route.target == null || route.target.isEmpty()) {
            log.warn("Route {} has no target destinations, route will be ignored", route.source)
            return
        }

        route.target.forEach { destination ->
            if (destination.uri.isNullOrBlank()) {
                throw ProxyException("Target URI cannot be null or blank for route: ${route.source}")
            }

            // 验证URI格式
            try {
                URI(destination.uri)
            } catch (_: Exception) {
                throw ProxyException("Invalid target URI format '${destination.uri}' for route: ${route.source}")
            }

            // 验证权重
            if (destination.weight <= 0) {
                throw ProxyException("Target weight must be positive for ${destination.uri} in route: ${route.source}")
            }
        }
    }

    /**
     * 查找匹配的代理配置
     *
     * @param requestPath 请求路径
     * @return 匹配的配置，如果没有匹配则返回null
     */
    fun findProxyConfig(requestPath: String): ProxyConfigMatch? {
        if (requestPath.isBlank()) {
            log.debug("Request path is null or blank, cannot find proxy configuration")
            return null
        }

        return routeMap[requestPath]
    }

    /**
     * 记录路由配置摘要
     */
    private fun logRouteSummary() {
        if (log.isInfoEnabled && routeMap.isNotEmpty()) {
            val summary = StringBuilder("\nProxy Route Configuration Summary:\n")
            summary.append("=====================================\n")
            routeMap.forEach { (source, config) ->
                summary.append("Source: ").append(source).append("\n")
                summary.append("  Targets: ")
                config.targets.forEach { target ->
                    summary.append(target.uri).append("(w:").append(target.weight).append(") ")
                }
                summary.append("\n")
            }
            summary.append("=====================================")
            log.info(summary.toString())
        }
    }

    /**
     * 代理配置匹配结果
     */
    data class ProxyConfigMatch(
        val route: ProxyRoute,
        val targets: List<ProxyDestination>
    )
}
