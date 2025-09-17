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
     * 路由配置列表
     * 默认值：null (需要在配置文件中明确指定)
     */
    private List<ProxyRoute> route;
}
