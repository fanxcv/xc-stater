package fun.fan.xc.plugin.proxy.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代理路由配置
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyRoute {

    /**
     * 源接口地址
     * 默认值：空字符串 (需要在配置中明确指定)
     */
    private String source = "";

    /**
     * 目标地址列表
     * 默认值：null (需要在配置中明确指定)
     */
    private List<ProxyDestination> target;
}