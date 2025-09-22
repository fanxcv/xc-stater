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
     * 默认值：30000ms (30秒)
     */
    private long timeout = 30000;

    /**
     * 总连接数
     * 默认值：10
     */
    private int maxConnections = 10;

    /**
     * 等待队列长度
     * 默认值：20
     */
    private int maxWaitQueueSize = 20;

    /**
     * 等待时间，单位ms
     * 默认值：10000ms (10s)
     */
    private long maxWaitTimeout = 10000L;

    /**
     * 路由配置列表
     * 默认值：null (需要在配置文件中明确指定)
     */
    private List<ProxyRoute> route;
}
