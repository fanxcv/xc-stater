package fun.fan.xc.plugin.proxy.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proxy Mock配置
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyMock {

    /**
     * 是否启用mock
     * 默认值：false
     */
    private Boolean enabled = false;

    /**
     * mock数据源，支持文件路径或远程URL
     * 格式：
     * - 文件路径：file:/path/to/mock.yaml
     * - 远程URL：url:https://example.com/mock.yaml
     * 默认值：null
     */
    private String dataSource;
}