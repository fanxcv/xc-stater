package `fun`.fan.xc.plugin.proxy.handler

import `fun`.fan.xc.plugin.proxy.client.ProxyClient
import `fun`.fan.xc.plugin.proxy.exception.ProxyException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
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
open class ProxyLoadBalancer(
    private val proxyClient: ProxyClient = ProxyClient(),
    private val maxRetryCount: Int = 3 // 最大重试次数
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyLoadBalancer::class.java)

    // 权重状态缓存，按目标URL列表的哈希值存储
    private val weightStates = mutableMapOf<String, WeightState>()

    /**
     * 执行负载均衡的代理请求
     *
     * @param request 原始HTTP请求
     * @param targetUrls 目标URL列表（包含权重）
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理响应
     */
    fun executeLoadBalancedRequest(
        request: io.netty.handler.codec.http.FullHttpRequest,
        targetUrls: List<WeightedUrl>,
        timeoutMs: Long = 5000
    ): CompletableFuture<ProxyClient.ProxyResponse> {
        if (targetUrls.isEmpty()) {
            val future = CompletableFuture<ProxyClient.ProxyResponse>()
            future.completeExceptionally(ProxyException("No target URLs available"))
            return future
        }

        // 计算总权重
        val totalWeight = targetUrls.sumOf { it.weight }

        // 获取或创建权重状态（按目标URL列表的哈希值）
        val targetsKey = targetUrls.joinToString("|") { "${it.url}:${it.weight}" }
        val weightState = weightStates.getOrPut(targetsKey) {
            WeightState(targetUrls, totalWeight)
        }

        log.debug(
            "Executing load balanced request with {} targets (total weight: {})",
            targetUrls.size, totalWeight
        )

        // 执行请求（支持失败重试）
        return executeWithRetry(request, weightState, timeoutMs)
    }

    /**
     * 执行带重试的请求
     */
    private fun executeWithRetry(
        request: io.netty.handler.codec.http.FullHttpRequest,
        weightState: WeightState,
        timeoutMs: Long
    ): CompletableFuture<ProxyClient.ProxyResponse> {
        val future = CompletableFuture<ProxyClient.ProxyResponse>()
        executeNextAttempt(request, weightState, timeoutMs, future)
        return future
    }

    /**
     * 执行下一次尝试
     */
    private fun executeNextAttempt(
        request: io.netty.handler.codec.http.FullHttpRequest,
        weightState: WeightState,
        timeoutMs: Long,
        future: CompletableFuture<ProxyClient.ProxyResponse>,
        retryCount: Int = 0 // 当前重试次数
    ) {
        // 检查是否超过最大重试次数
        if (retryCount >= maxRetryCount) {
            val errorMsg = "All target URLs failed after $maxRetryCount retries"
            log.error(errorMsg)
            future.completeExceptionally(RuntimeException(errorMsg))
            return
        }

        val selectedUrl = weightState.selectNext()

        if (selectedUrl == null) {
            // 所有URL都已尝试过，但都失败了
            val errorMsg = "All target URLs failed after retries"
            log.error(errorMsg)
            future.completeExceptionally(RuntimeException(errorMsg))
            return
        }

        log.debug("Attempting request to: {} (weight: {})", selectedUrl.url, selectedUrl.weight)

        // 执行单个请求
        proxyClient.executeProxyRequest(request, selectedUrl.url, timeoutMs)
            .whenComplete { response, throwable ->
                if (throwable == null && response != null) {
                    // 请求成功
                    if (isSuccessResponse(response.statusCode)) {
                        log.debug(
                            "Request succeeded to: {} with status: {}",
                            selectedUrl.url, response.statusCode
                        )
                        // 成功响应后重置失败标记
                        weightState.resetFailedUrls()
                        if (!future.isDone) {
                            future.complete(response)
                        }
                    } else {
                        // 响应状态码表示失败，标记URL并尝试下一个
                        log.warn(
                            "Request failed to: {} with status: {}, trying next target",
                            selectedUrl.url, response.statusCode
                        )
                        weightState.markUrlAsFailed(selectedUrl.url)
                        if (!future.isDone) {
                            executeNextAttempt(request, weightState, timeoutMs, future, retryCount + 1)
                        }
                    }
                } else {
                    // 请求失败，尝试下一个URL
                    val errorMsg = if (throwable != null) {
                        "Request exception to: ${selectedUrl.url} : ${throwable.message}"
                    } else {
                        "Request failed to: ${selectedUrl.url} with null response"
                    }

                    // 根据异常类型记录不同级别的日志
                    when (throwable) {
                        is java.util.concurrent.TimeoutException -> {
                            log.warn("Timeout {}", errorMsg)
                        }
                        is java.net.ConnectException -> {
                            log.warn("Connection failed {}", errorMsg)
                        }
                        is java.net.UnknownHostException -> {
                            log.warn("Unknown host {}", errorMsg)
                        }
                        else -> {
                            log.warn("General error {}", errorMsg)
                        }
                    }

                    weightState.markUrlAsFailed(selectedUrl.url)
                    if (!future.isDone) {
                        executeNextAttempt(request, weightState, timeoutMs, future, retryCount + 1)
                    }
                }
            }
    }

    /**
     * 判断响应是否成功
     */
    private fun isSuccessResponse(statusCode: Int): Boolean {
        return statusCode in 200..299
    }

    /**
     * 关闭负载均衡器
     */
    fun shutdown() {
        log.info("Shutting down proxy load balancer...")
        proxyClient.shutdown()
        log.info("Proxy load balancer shutdown completed")
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
        private val log: Logger = LoggerFactory.getLogger(ProxyLoadBalancer::class.java)

        private val failedUrls = mutableSetOf<String>()
        private val currentWeights = weightedUrls.map { it.weight }.toMutableList() // 当前权重列表

        /**
         * 选择下一个URL（使用标准的平滑加权轮询算法）
         */
        fun selectNext(): WeightedUrl? {
            // 如果所有URL都已失败，返回null
            val availableUrls = weightedUrls.filter { it.url !in failedUrls }
            if (availableUrls.isEmpty()) {
                return null
            }

            // 如果只有一个可用的URL，直接返回
            if (availableUrls.size == 1) {
                val selected = availableUrls.first()
                log.debug("Selected URL (only one available): {}", selected.url)
                return selected
            }

            // 标准平滑加权轮询算法
            var selectedUrl: WeightedUrl? = null
            var selectedIndex = -1
            var maxCurrentWeight = -1

            // 遍历所有可用的URL
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

            if (selectedUrl != null && selectedIndex >= 0) {
                // 被选中的URL减去总权重
                currentWeights[selectedIndex] -= totalWeight

                log.debug(
                    "Selected URL: {} with weight: {} (current weights: {})",
                    selectedUrl.url, selectedUrl.weight, currentWeights.joinToString()
                )
                log.info("Load balancer selected target: {} with weight: {}", selectedUrl.url, selectedUrl.weight)
                return selectedUrl
            }

            // 如果算法有问题（理论上不会发生），退化为随机选择
            val selected = availableUrls[Random.nextInt(availableUrls.size)]
            log.debug("Selected URL (fallback random): {} with weight: {}", selected.url, selected.weight)
            return selected
        }

        /**
         * 标记URL为失败（在重试时调用）
         */
        fun markUrlAsFailed(url: String) {
            failedUrls.add(url)
            log.debug("Marked URL as failed: {}", url)
        }

        /**
         * 重置失败标记（在成功时调用）
         */
        fun resetFailedUrls() {
            failedUrls.clear()
            log.debug("Reset all failed URLs")
        }
    }
}
