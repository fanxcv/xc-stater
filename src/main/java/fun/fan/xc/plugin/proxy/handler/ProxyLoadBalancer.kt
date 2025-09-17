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
 */
open class ProxyLoadBalancer(private val proxyClient: ProxyClient = ProxyClient()) {

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
        future: CompletableFuture<ProxyClient.ProxyResponse>
    ) {
        val selectedUrl = urlSelector.selectNext()

        if (selectedUrl == null) {
            // 所有URL都已尝试过，但都失败了
            future.completeExceptionally(Exception("All target URLs failed after retries"))
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
                            executeNextAttempt(request, urlSelector, timeoutMs, future)
                        }
                    }
                } else {
                    // 请求失败，尝试下一个URL
                    log.warn(
                        "Request exception to: {} : {}, trying next target",
                        selectedUrl.url, throwable?.message
                    )
                    if (!future.isDone) {
                        executeNextAttempt(request, urlSelector, timeoutMs, future)
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

        /**
         * 选择下一个URL
         */
        fun selectNext(): WeightedUrl? {
            // 如果所有URL都已尝试过，返回null
            val availableUrls = weightedUrls.filter { it.url !in attemptedUrls }
            if (availableUrls.isEmpty()) {
                return null
            }

            // 权重轮询算法
            var randomWeight = Random.nextInt(totalWeight)
            for (weightedUrl in availableUrls) {
                randomWeight -= weightedUrl.weight
                if (randomWeight < 0) {
                    attemptedUrls.add(weightedUrl.url)
                    log.debug(
                        "Selected URL: {} with weight: {} (remaining: {}/{})",
                        weightedUrl.url, weightedUrl.weight,
                        availableUrls.size - 1, weightedUrls.size
                    )
                    return weightedUrl
                }
            }

            // 如果权重计算有问题，返回第一个可用的URL
            val selected = availableUrls.first()
            attemptedUrls.add(selected.url)
            log.debug("Selected URL (fallback): {} with weight: {}", selected.url, selected.weight)
            return selected
        }
    }
}
