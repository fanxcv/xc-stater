package fun.fan.xc.plugin.proxy.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mock配置项
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockConfig {

    /**
     * 请求匹配配置
     */
    private MockRequest request;

    /**
     * 响应配置
     */
    private MockResponse response;
}