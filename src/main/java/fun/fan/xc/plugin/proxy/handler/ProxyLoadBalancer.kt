package `fun`.fan.xc.plugin.proxy.handler

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 代理负载均衡器
 * 支持权重轮询算法和失败重试机制
 *
 * @author fan
 *
 * ## 功能特性
 * - 支持权重轮询算法，根据权重分配请求
 * - 支持失败重试机制，提高系统可用性
 * - 支持平滑加权轮询算法，确保权重分配准确性
 * - 可配置最大重试次数，防止无限重试
 *
 * ## 使用示例
 * ```kotlin
 * val loadBalancer = ProxyLoadBalancer()
 * val urls = listOf(WeightedUrl("http://server1.com", 5), WeightedUrl("http://server2.com", 3))
 * val response = loadBalancer.executeLoadBalancedRequest(request, urls).get()
 * ```
 */
open class ProxyLoadBalancer(private val key: String) {
    companion object {
        val loadBalancerCache = mutableMapOf<String, ProxyLoadBalancer>()

        fun getOrCreateLoadBalancer(routeKey: String): ProxyLoadBalancer {
            return loadBalancerCache.getOrPut(routeKey) {
                ProxyLoadBalancer(routeKey)
            }
        }
    }

    // 权重状态缓存，按目标URL列表的哈希值存储
    private val weightStates = mutableMapOf<String, WeightState>()

    // 总权重缓存，按目标URL列表的哈希值存储
    private val totalWeightCache = mutableMapOf<String, Int>()

    /**
     * 标记指定路由下的URL为故障状态
     */
    fun markFailed(failedUrl: String) {
        val weightState = weightStates[key]
        weightState?.markFailed(failedUrl)
    }

    /**
     * 清除指定路由下所有URL的故障状态
     */
    fun clearAllFailures() {
        val weightState = weightStates[key]
        weightState?.clearAllFailures()
    }

    /**
     * 检查指定路由下是否所有URL都故障了
     */
    fun areAllUrlsFailed(): Boolean {
        val weightState = weightStates[key]
        return weightState?.areAllUrlsFailed() ?: false
    }

    /**
     * 同步选择目标URL（用于ProxyOrchestrator）
     *
     * @param targetUrls 目标URL列表（包含权重）
     * @return 选中的目标URL
     */
    fun selectTarget(targetUrls: List<WeightedUrl>): WeightedUrl? {
        if (targetUrls.isEmpty()) {
            return null
        }

        // 获取或创建总权重（按目标URL列表的哈希值）
        val totalWeight = totalWeightCache.getOrPut(key) {
            targetUrls.sumOf { it.weight }
        }

        // 获取或创建权重状态（按目标URL列表的哈希值）
        val weightState = weightStates.getOrPut(key) {
            WeightState(targetUrls, totalWeight)
        }

        // 直接选择下一个URL（不执行重试逻辑）
        return weightState.selectNext()
    }

    /**
     * 加权URL包装类
     */
    data class WeightedUrl(
        val url: String,
        val weight: Int
    )

    /**
     * 权重状态管理类（用于持久化权重状态）
     */
    private class WeightState(
        private val weightedUrls: List<WeightedUrl>,
        private val totalWeight: Int
    ) {
        // private val lock = ReentrantReadWriteLock()
        private val failedUrls = ConcurrentHashMap<String, Long>() // 故障URL及其失败时间戳
        private val currentWeights = weightedUrls.map { it.weight }.toMutableList() // 当前权重列表
        private val failureTimeoutMs = 30000L // 故障超时时间，30秒后自动恢复

        /**
         * 标记URL为故障状态
         */
        fun markFailed(url: String) {
            failedUrls[url] = System.currentTimeMillis()
        }

        /**
         * 清除所有故障状态
         */
        fun clearAllFailures() {
            failedUrls.clear()
        }

        /**
         * 检查并清理过期的故障标记
         */
        private fun cleanupExpiredFailures() {
            val now = System.currentTimeMillis()
            failedUrls.entries.removeIf { entry ->
                now - entry.value > failureTimeoutMs
            }
        }

        /**
         * 获取当前可用的URL列表
         */
        private fun getAvailableUrls(): List<WeightedUrl> {
            cleanupExpiredFailures()
            return weightedUrls.filter { it.url !in failedUrls.keys }
        }

        /**
         * 检查是否所有URL都故障了
         */
        fun areAllUrlsFailed(): Boolean {
            cleanupExpiredFailures()
            return failedUrls.size >= weightedUrls.size
        }

        /**
         * 选择下一个URL（使用优化的平滑加权轮询算法）
         */
        fun selectNext(): WeightedUrl? {
            // 清理过期的故障标记
            cleanupExpiredFailures()

            // 获取可用的URL列表
            val availableUrls = getAvailableUrls()

            // 如果所有URL都已失败，返回null
            if (availableUrls.isEmpty()) {
                return null
            }

            // 如果只有一个可用的URL，直接返回
            if (availableUrls.size == 1) {
                return availableUrls.first()
            }

            // 优化的平滑加权轮询算法
            var selectedUrl: WeightedUrl? = null
            var selectedIndex = -1
            var maxCurrentWeight = Int.MIN_VALUE

            // 遍历所有可用的URL，找到当前权重最大的URL
            for (i in weightedUrls.indices) {
                val url = weightedUrls[i]
                // 跳过已失败的URL
                if (url.url in failedUrls.keys) {
                    continue
                }

                // 当前权重 = 当前权重 + 实际权重
                currentWeights[i] += url.weight

                // 选择当前权重最大的URL
                if (currentWeights[i] > maxCurrentWeight) {
                    maxCurrentWeight = currentWeights[i]
                    selectedUrl = url
                    selectedIndex = i
                }
            }

            if (selectedUrl != null) {
                // 被选中的URL减去总权重
                currentWeights[selectedIndex] -= totalWeight
                return selectedUrl
            }

            // 如果算法有问题（理论上不会发生），退化为随机选择
            val selected = availableUrls[Random.nextInt(availableUrls.size)]
            return selected
        }
    }
}
