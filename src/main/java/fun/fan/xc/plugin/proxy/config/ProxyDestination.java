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
     */
    private String uri = "";
    
    /**
     * 权重
     */
    private int weight = 1;
}