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

        // 创建选择器（包含重试逻辑）
        val urlSelector = WeightedUrlSelector(targetUrls, totalWeight)

        log.debug(
            "Executing load balanced request with {} targets (total weight: {})",
            targetUrls.size, totalWeight
        )

        // 执行请求（支持失败重试）
        return executeWithRetry(request, urlSelector, timeoutMs)
    }

    /**
     * 执行带重试的请求
     */
    private fun executeWithRetry(
        request: io.netty.handler.codec.http.FullHttpRequest,
        urlSelector: WeightedUrlSelector,
        timeoutMs: Long
    ): CompletableFuture<ProxyClient.ProxyResponse> {
        val future = CompletableFuture<ProxyClient.ProxyResponse>()
        executeNextAttempt(request, urlSelector, timeoutMs, future)
        return future
    }

    /**
     * 执行下一次尝试
     */
    private fun executeNextAttempt(
        request: io.netty.handler.codec.http.FullHttpRequest,
        urlSelector: WeightedUrlSelector,
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

        val selectedUrl = urlSelector.selectNext()

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
                        if (!future.isDone) {
                            future.complete(response)
                        }
                    } else {
                        // 响应状态码表示失败，尝试下一个URL
                        log.warn(
                            "Request failed to: {} with status: {}, trying next target",
                            selectedUrl.url, response.statusCode
                        )
                        if (!future.isDone) {
                            executeNextAttempt(request, urlSelector, timeoutMs, future, retryCount + 1)
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

                    if (!future.isDone) {
                        executeNextAttempt(request, urlSelector, timeoutMs, future, retryCount + 1)
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
     * 权重URL选择器
     */
    private class WeightedUrlSelector(
        private val weightedUrls: List<WeightedUrl>,
        private val totalWeight: Int
    ) {
        private val log: Logger = LoggerFactory.getLogger(ProxyLoadBalancer::class.java)

        private val attemptedUrls = mutableSetOf<String>()
        private var currentPos = 0 // 当前位置，用于平滑加权轮询
        private val effectiveWeights = weightedUrls.map { it.weight }.toMutableList() // 有效权重列表

        /**
         * 选择下一个URL（使用平滑加权轮询算法）
         */
        fun selectNext(): WeightedUrl? {
            // 如果所有URL都已尝试过，返回null
            val availableUrls = weightedUrls.filter { it.url !in attemptedUrls }
            if (availableUrls.isEmpty()) {
                return null
            }

            // 平滑加权轮询算法
            var totalEffectiveWeight = effectiveWeights.sum()
            if (totalEffectiveWeight <= 0) {
                // 如果所有权重都<=0，则退化为随机选择
                val selected = availableUrls[Random.nextInt(availableUrls.size)]
                attemptedUrls.add(selected.url)
                return selected
            }

            // 计算下一个服务器
            var selectedUrl: WeightedUrl? = null
            var selectedIndex = -1
            var maxWeight = -1

            for (i in weightedUrls.indices) {
                val url = weightedUrls[i]
                // 跳过已尝试过的URL
                if (url.url in attemptedUrls) {
                    continue
                }

                // 增加当前权重
                effectiveWeights[i] += url.weight
                // 选择权重最大的
                if (effectiveWeights[i] > maxWeight) {
                    maxWeight = effectiveWeights[i]
                    selectedUrl = url
                    selectedIndex = i
                }
            }

            if (selectedUrl != null && selectedIndex >= 0) {
                // 减去总权重
                effectiveWeights[selectedIndex] -= totalEffectiveWeight
                attemptedUrls.add(selectedUrl.url)
                log.debug(
                    "Selected URL: {} with weight: {} (remaining: {}/{})",
                    selectedUrl.url, selectedUrl.weight,
                    availableUrls.size - 1, weightedUrls.size
                )
                log.info("Load balancer selected target: {} with weight: {}", selectedUrl.url, selectedUrl.weight)
                return selectedUrl
            }

            // 如果算法有问题，返回第一个可用的URL
            val selected = availableUrls.first()
            attemptedUrls.add(selected.url)
            log.debug("Selected URL (fallback): {} with weight: {}", selected.url, selected.weight)
            return selected
        }
    }
}
