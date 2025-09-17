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
     */
    private int timeout = 3000;

    /**
     * 路由配置列表
     */
    private List<ProxyRoute> route;
}
