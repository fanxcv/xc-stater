package `fun`.fan.xc.plugin.proxy.client

import `fun`.fan.xc.plugin.proxy.config.ProxyProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 代理客户端门面类
 * 提供简单的代理请求接口，内部委托给专门的执行器处理
 *
 * @author fan
 *
 * ## 职责范围
 * - 提供统一的代理请求接口
 * - 委托HTTP执行给ProxyHttpExecutor
 * - 简化客户端使用复杂性
 * - 提供兼容性支持（保持原有API接口）
 *
 * ## 使用示例
 * ```kotlin
 * val client = ProxyClient(connectionPool, properties)
 * val response = client.executeProxyRequest(request, "http://example.com")
 * ```
 */
class ProxyClient(
    private val connectionPool: HostPortChannelPool,
    private val properties: ProxyProperties
) {

    private val log: Logger = LoggerFactory.getLogger(ProxyClient::class.java)
    private val httpExecutor: ProxyHttpExecutor = ProxyHttpExecutor(connectionPool, properties)

    /**
     * 执行代理请求（委托给专门的HTTP执行器）
     *
     * @param request HTTP请求对象
     * @param targetUrl 目标URL
     * @param connection 已获取的连接（可选）
     * @param timeoutMs 超时时间(毫秒)
     * @return 代理响应
     * @throws java.util.concurrent.TimeoutException 请求超时
     * @throws Exception 执行失败异常
     */
    suspend fun executeProxyRequest(
        request: io.netty.handler.codec.http.FullHttpRequest,
        targetUrl: String,
        connection: io.netty.channel.Channel? = null,
        timeoutMs: Long = 5000
    ): ProxyResponse {
        log.debug("Delegating proxy request to HTTP executor: {}, timeout: {}ms", targetUrl, timeoutMs)
        val executorResponse = httpExecutor.executeHttpRequest(request, targetUrl, connection, timeoutMs)
        return ProxyResponse(executorResponse.statusCode, executorResponse.headers, executorResponse.body, targetUrl)
    }

    /**
     * 代理响应数据类（兼容性，委托给ProxyHttpExecutor.ProxyResponse）
     */
    data class ProxyResponse(
        val statusCode: Int,
        val headers: Map<String, String>,
        val body: ByteArray,
        val target: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ProxyResponse

            if (statusCode != other.statusCode) return false
            if (headers != other.headers) return false
            if (!body.contentEquals(other.body)) return false
            if (target != other.target) return false

            return true
        }

        override fun hashCode(): Int {
            var result = statusCode
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            result = 31 * result + target.hashCode()
            return result
        }
    }
}
