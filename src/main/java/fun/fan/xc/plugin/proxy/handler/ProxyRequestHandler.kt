package `fun`.fan.xc.plugin.proxy.handler

import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.config.ProxyDestination
import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import `fun`.fan.xc.plugin.proxy.config.ProxyRoute
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.coroutines.*

/**
 * 代理请求处理器
 * 处理Spring MVC请求，根据配置转发到目标服务器
 *
 * @author fan
 */
open class ProxyRequestHandler(
    private val proxyProperties: ProxyProperties,
    private val proxyClient: ProxyClient
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyRequestHandler::class.java)

    // 负载均衡器缓存
    private val loadBalancerCache = mutableMapOf<String, ProxyLoadBalancer>()

    // 路由映射缓存
    private val routeMap = mutableMapOf<String, ProxyConfigMatch>()

    init {
        initializeRouteMap()
    }

    /**
     * 初始化路由映射
     */
    private fun initializeRouteMap() {
        proxyProperties.route?.forEach { route ->
            if (route.target != null && route.target.isNotEmpty()) {
                routeMap[route.source] = ProxyConfigMatch(route, route.target)
                log.debug(
                    "Initialized route mapping: source={} -> targets={}",
                    route.source, route.target.map { it.uri })
            }
        }
        log.info("Route map initialized with {} routes", routeMap.size)
    }

    /**
     * 处理代理请求
     *
     * @param requestPath 请求路径
     * @param request HTTP请求
     * @return 代理响应
     */
    suspend fun handleProxyRequest(
        requestPath: String,
        request: io.netty.handler.codec.http.FullHttpRequest
    ): ProxyClient.ProxyResponse {
        // 查找匹配的代理配置
        val matchedConfig = findMatchingProxyConfig(requestPath)
            ?: throw ProxyException("No proxy configuration found for path: $requestPath")

        log.debug(
            "Found proxy configuration: source={}, targets={}",
            matchedConfig.route.source,
            matchedConfig.targets.map { it.uri })

        // 构建目标URL列表
        val weightedUrls = matchedConfig.targets.map { target ->
            ProxyLoadBalancer.WeightedUrl(target.uri, target.weight)
        }

        // 获取或创建负载均衡器
        val loadBalancerKey = matchedConfig.route.source
        val loadBalancer = loadBalancerCache.getOrPut(loadBalancerKey) {
            ProxyLoadBalancer(proxyClient)
        }

        // 执行负载均衡请求
        return loadBalancer.executeLoadBalancedRequest(request, weightedUrls, proxyProperties.timeout.toLong())
    }

    /**
     * 查找匹配的代理配置
     */
    fun findMatchingProxyConfig(requestPath: String): ProxyConfigMatch? {
        // 直接从路由映射缓存中查找
        return routeMap[requestPath]
    }

    /**
     * 获取所有配置的代理路径
     */
    fun getConfiguredPaths(): List<String> {
        return proxyProperties.route?.map { it.source }?.sorted() ?: emptyList()
    }

    /**
     * 刷新配置缓存
     */
    fun refreshConfig() {
        log.info("Refreshing proxy configuration cache...")

        // 重新初始化路由映射
        routeMap.clear()
        initializeRouteMap()

        // 清理不可用路由的负载均衡器
        val currentRouteKeys = proxyProperties.route?.map { it.source }?.toSet() ?: emptySet()

        // 移除不再存在的负载均衡器
        loadBalancerCache.keys.removeAll { it !in currentRouteKeys }

        log.info(
            "Proxy configuration cache refreshed, active load balancers: {}, routes: {}",
            loadBalancerCache.size, routeMap.size
        )
    }

    /**
     * 代理配置匹配结果
     */
    data class ProxyConfigMatch(
        val route: ProxyRoute,
        val targets: List<ProxyDestination>
    )
}
