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
     */
    private String source = "";
    
    /**
     * 目标地址列表
     */
    private List<ProxyDestination> target;
}