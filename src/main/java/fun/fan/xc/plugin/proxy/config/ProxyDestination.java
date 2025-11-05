package fun.fan.xc.plugin.proxy.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理目标配置
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDestination {

    /**
     * 目标URI地址
     * 默认值：空字符串 (需要在配置中明确指定)
     */
    private String uri = "";

    /**
     * 权重
     * 默认值：1 (最低权重)
     */
    private int weight = 1;

    /**
     * HTTP对象聚合器最大大小，单位：字节
     * 默认值：null (如果未设置，则使用路由级别配置或全局默认值 2MB)
     * 用于控制HTTP请求/响应消息体的最大聚合大小
     * 优先级：目标级配置 > 路由级配置 > 全局默认值
     */
    private Integer maxAggregatorSize;
}
