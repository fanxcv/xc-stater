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
}