package fun.fan.xc.plugin.proxy.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 代理配置属性
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "xc.proxy")
public class ProxyProperties {

    /**
     * 超时时间，单位ms
     * 默认值：3000ms (3秒)
     */
    private int timeout = 3000;

    /**
     * Netty连接池配置
     * 默认值：使用合理的默认配置
     */
    private NettyPoolProperties pool = new NettyPoolProperties();

    /**
     * 路由配置列表
     * 默认值：null (需要在配置文件中明确指定)
     */
    private List<ProxyRoute> route;

    /**
     * Netty连接池配置属性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NettyPoolProperties {
        /**
         * 最大总连接数
         * 默认值：200
         */
        private int maxTotalConnections = 200;

        /**
         * 连接空闲超时时间(毫秒)
         * 默认值：300000 (5分钟)
         */
        private long connectionIdleTimeout = 300000L;

        /**
         * 连接超时时间(毫秒)
         * 默认值：2000 (2秒)
         */
        private long connectionTimeout = 2000L;

        /**
         * 最大等待队列长度
         * 默认值：50
         */
        private int maxWaitQueueSize = 50;

        /**
         * 健康检查间隔(毫秒)
         * 默认值：60000 (1分钟)
         */
        private long healthCheckInterval = 60000L;

        /**
         * 连接创建失败重试次数
         * 默认值：3
         */
        private int connectRetryCount = 3;
    }
}
