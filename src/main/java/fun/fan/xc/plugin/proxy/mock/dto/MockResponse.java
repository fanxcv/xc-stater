package fun.fan.xc.plugin.proxy.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Mock响应配置
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockResponse {

    /**
     * HTTP响应状态码
     * 默认值：200
     */
    private Integer status = 200;

    /**
     * 响应头
     * key为Header名称，value为Header值
     * 可选配置
     */
    private Map<String, String> headers;

    /**
     * 响应体内容
     * 支持文本、JSON、XML等格式
     */
    private String body;
}