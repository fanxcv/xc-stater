package fun.fan.xc.plugin.proxy.mock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mock数据根配置
 * 对应YAML文件中的根结构
 *
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockData {

    /**
     * Mock配置列表
     */
    private List<MockConfig> mocks;
}