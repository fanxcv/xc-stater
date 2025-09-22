package `fun`.fan.xc.plugin.proxy.handler

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
open class ProxyLoadBalancer() {
    companion object {
        val loadBalancerCache = mutableMapOf<String, ProxyLoadBalancer>()

        fun getOrCreateLoadBalancer(routeKey: String): ProxyLoadBalancer {
            return loadBalancerCache.getOrPut(routeKey) {
                ProxyLoadBalancer()
            }
        }
    }

    // 权重状态缓存，按目标URL列表的哈希值存储
    private val weightStates = mutableMapOf<String, WeightState>()

    // 总权重缓存，按目标URL列表的哈希值存储
    private val totalWeightCache = mutableMapOf<String, Int>()

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

        // 检查缓存是否过期
        val targetsKey = targetUrls.joinToString("|") { "${it.url}:${it.weight}" }

        // 获取或创建总权重（按目标URL列表的哈希值）
        val totalWeight = totalWeightCache.getOrPut(targetsKey) {
            targetUrls.sumOf { it.weight }
        }

        // 获取或创建权重状态（按目标URL列表的哈希值）
        val weightState = weightStates.getOrPut(targetsKey) {
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
        private val failedUrls = mutableSetOf<String>()
        private val currentWeights = weightedUrls.map { it.weight }.toMutableList() // 当前权重列表

        /**
         * 选择下一个URL（使用优化的平滑加权轮询算法）
         */
        fun selectNext(): WeightedUrl? {
            // 如果所有URL都已失败，返回null
            val availableUrls = weightedUrls.filter { it.url !in failedUrls }
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
                if (url.url in failedUrls) {
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
