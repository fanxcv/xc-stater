package fun.fan.xc.plugin.proxy.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Mock请求匹配配置
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockRequest {

    /**
     * 请求路径
     * 支持Ant路径匹配，如：/api/**, /user/{id}
     */
    private String path;

    /**
     * HTTP请求方法
     * 如：GET, POST, PUT, DELETE等
     * 可选配置，不设置则匹配所有方法
     */
    private String method;

    /**
     * 请求参数匹配
     * key为参数名，value为期望值
     * 可选配置，不设置则不匹配参数
     */
    private Map<String, Object> params;
}
