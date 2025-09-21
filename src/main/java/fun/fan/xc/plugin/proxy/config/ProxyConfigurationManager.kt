package `fun`.fan.xc.plugin.proxy.config

import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        try {
            properties.route?.forEach { route ->
                validateRouteConfiguration(route)
                if (route.target != null && route.target.isNotEmpty()) {
                    routeMap[route.source] = ProxyConfigMatch(route, route.target)
                    log.debug("Added route mapping: {} -> {}", route.source, route.target.map { it.uri })
                }
            }
            log.info("Initialized {} proxy routes", routeMap.size)
            logRouteSummary()
        } catch (e: Exception) {
            log.error("Failed to initialize proxy route configuration", e)
            throw ProxyException("Failed to initialize proxy configuration: ${e.message}")
        }
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
                java.net.URI(destination.uri)
            } catch (e: Exception) {
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
     * 获取所有配置的路由信息
     */
    fun getAllConfiguredRoutes(): Map<String, ProxyConfigMatch> {
        return routeMap.toMap()
    }

    /**
     * 获取配置的路由数量
     */
    fun getRouteCount(): Int {
        return routeMap.size
    }

    /**
     * 检查指定路径是否配置了代理
     */
    fun isProxyConfigured(requestPath: String): Boolean {
        return routeMap.containsKey(requestPath)
    }

    /**
     * 获取指定路由的目标服务器列表
     */
    fun getTargetsForRoute(requestPath: String): List<ProxyDestination>? {
        val config = routeMap[requestPath]
        return config?.targets
    }

    /**
     * 重新加载配置（支持热重载）
     */
    fun reloadConfiguration() {
        log.info("Reloading proxy configuration...")
        routeMap.clear()
        initializeRouteMap()
        log.info("Proxy configuration reloaded successfully")
    }

    /**
     * 添加新的路由配置
     */
    fun addRoute(route: ProxyRoute): Boolean {
        try {
            validateRouteConfiguration(route)
            if (route.target != null && route.target.isNotEmpty()) {
                routeMap[route.source] = ProxyConfigMatch(route, route.target)
                log.info("Added route mapping: {} -> {}", route.source, route.target.map { it.uri })
                return true
            }
        } catch (e: Exception) {
            log.error("Failed to add route: {}", route.source, e)
        }
        return false
    }

    /**
     * 移除路由配置
     */
    fun removeRoute(sourcePath: String): Boolean {
        return if (routeMap.containsKey(sourcePath)) {
            routeMap.remove(sourcePath)
            log.info("Removed route mapping: {}", sourcePath)
            true
        } else {
            log.debug("Route mapping not found for removal: {}", sourcePath)
            false
        }
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
     * 获取配置的副本（防止外部修改）
     */
    fun getConfigurationSnapshot(): Map<String, ProxyConfigMatch> {
        return routeMap.toMap()
    }

    /**
     * 检查配置的健康状态
     */
    fun validateConfigurationHealth(): ConfigurationHealth {
        val issues = mutableListOf<String>()
        var healthyRouteCount = 0

        routeMap.forEach { (source, config) ->
            try {
                if (config.targets.isEmpty()) {
                    issues.add("Route '$source' has no target destinations")
                } else {
                    val validTargets = config.targets.count { target ->
                        try {
                            java.net.URI(target.uri)
                            target.weight > 0
                        } catch (e: Exception) {
                            false
                        }
                    }

                    if (validTargets == 0) {
                        issues.add("Route '$source' has no valid target destinations")
                    } else if (validTargets < config.targets.size) {
                        issues.add("Route '$source' has ${config.targets.size - validTargets} invalid target destinations")
                    } else {
                        healthyRouteCount++
                    }
                }
            } catch (e: Exception) {
                issues.add("Route '$source' validation failed: ${e.message}")
            }
        }

        return ConfigurationHealth(
            totalRoutes = routeMap.size,
            healthyRoutes = healthyRouteCount,
            issues = issues
        )
    }

    /**
     * 代理配置匹配结果
     */
    data class ProxyConfigMatch(
        val route: ProxyRoute,
        val targets: List<ProxyDestination>
    ) {
        /**
         * 获取目标的总权重
         */
        fun getTotalWeight(): Int {
            return targets.sumOf { it.weight }
        }

        /**
         * 获取目标地址列表
         */
        fun getTargetUris(): List<String> {
            return targets.map { it.uri }
        }

        /**
         * 检查是否有可用的目标
         */
        fun hasAvailableTargets(): Boolean {
            return targets.isNotEmpty()
        }
    }

    /**
     * 配置健康状态
     */
    data class ConfigurationHealth(
        val totalRoutes: Int,
        val healthyRoutes: Int,
        val issues: List<String>
    ) {
        /**
         * 检查配置是否健康
         */
        fun isHealthy(): Boolean {
            return issues.isEmpty() && healthyRoutes == totalRoutes
        }

        /**
         * 获取健康状态描述
         */
        fun getStatusDescription(): String {
            return when {
                isHealthy() -> "All routes are healthy"
                healthyRoutes == 0 -> "No healthy routes available"
                healthyRoutes < totalRoutes -> "$healthyRoutes of $totalRoutes routes are healthy"
                else -> "Configuration has issues"
            }
        }
    }
}
