package fun.fan.xc.plugin.proxy.mock;

import fun.fan.xc.plugin.proxy.mock.dto.MockConfig;
import fun.fan.xc.plugin.proxy.mock.dto.MockData;
import fun.fan.xc.plugin.proxy.mock.dto.MockRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Proxy Mock智能匹配器
 * 实现6级优先级匹配算法
 *
 * @author fan
 */
@Slf4j
public class ProxyMockMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 根据请求信息匹配最佳mock配置
     *
     * @param requestPath   请求路径
     * @param requestMethod 请求方法
     * @param requestParams 请求参数（从Event对象获取）
     * @param mockData      Mock数据配置
     * @return 最佳匹配的mock配置，如果没有匹配返回null
     */
    public MockConfig findBestMatch(String requestPath, String requestMethod,
                                    Map<String, Object> requestParams,
                                    MockData mockData) {
        if (mockData == null || CollectionUtils.isEmpty(mockData.getMocks())) {
            log.debug("Mock配置为空或无配置项");
            return null;
        }

        List<MockConfig> mockConfigs = mockData.getMocks();

        return mockConfigs.stream()
                .filter(config -> isPathMatch(config, requestPath))
                .max(Comparator.comparingInt(config -> calculatePriority(config, requestMethod, requestParams)))
                .orElse(null);
    }

    /**
     * 检查路径是否匹配
     */
    private boolean isPathMatch(MockConfig config, String requestPath) {
        MockRequest request = config.getRequest();
        if (request == null || !org.springframework.util.StringUtils.hasText(request.getPath())) {
            return false;
        }

        return pathMatcher.match(request.getPath(), requestPath);
    }

    /**
     * 计算匹配优先级
     * 优先级从高到低：
     * 1. 路径 + 方法 + 参数数量（参数匹配最多）
     * 2. 路径 + 方法 + 部分参数（参数匹配度高的优先）
     * 3. 路径 + 方法
     * 4. 路径 + 参数数量（参数匹配最多）
     * 5. 路径 + 部分参数（参数匹配度高的优先）
     * 6. 路径
     */
    private int calculatePriority(MockConfig config, String requestMethod, Map<String, Object> requestParams) {
        MockRequest request = config.getRequest();
        int priority = 0;

        // 基础路径匹配得分
        priority += 100000;

        // 方法匹配得分（优先级很高）
        boolean methodMatched = isMethodMatched(request, requestMethod);
        if (methodMatched) {
            priority += 50000;
        }

        // 参数匹配得分计算
        if (request.getParams() != null && !request.getParams().isEmpty()) {
            int configParamCount = request.getParams().size();
            int matchedParamCount = calculateMatchedParams(request.getParams(), requestParams);

            // 参数匹配数量得分（主要得分）
            priority += matchedParamCount * 1000;

            // 参数匹配度得分（精细化得分）
            double matchRatio = (double) matchedParamCount / configParamCount;
            priority += (int) (matchRatio * 500);

            log.debug("参数匹配情况: 配置参数数={}, 匹配参数数={}, 匹配度={}, 优先级得分={}",
                    configParamCount, matchedParamCount, matchRatio, priority);
        }

        log.debug("Mock配置匹配计算: 路径={}, 方法={}, 优先级得分={}",
                request.getPath(), request.getMethod(), priority);

        return priority;
    }

    /**
     * 检查方法是否匹配
     */
    private boolean isMethodMatched(MockRequest request, String requestMethod) {
        if (request.getMethod() == null) {
            // 如果配置中没有指定方法，则匹配所有方法
            return true;
        }
        return request.getMethod().equalsIgnoreCase(requestMethod);
    }

    /**
     * 计算匹配的参数数量
     */
    private int calculateMatchedParams(Map<String, Object> configParams, Map<String, Object> requestParams) {
        if (CollectionUtils.isEmpty(configParams) || CollectionUtils.isEmpty(requestParams)) {
            return 0;
        }

        return (int) configParams.entrySet().stream()
                .filter(entry -> {
                    Object requestValue = requestParams.get(entry.getKey());
                    return requestValue != null && requestValue.equals(entry.getValue());
                })
                .count();
    }

    /**
     * 获取匹配详情（用于调试）
     */
    public String getMatchDetails(String requestPath, String requestMethod,
                                  Map<String, Object> requestParams,
                                  MockData mockData) {
        if (mockData == null || CollectionUtils.isEmpty(mockData.getMocks())) {
            return "无Mock配置";
        }

        StringBuilder details = new StringBuilder();
        details.append("请求信息: 路径=").append(requestPath)
                .append(", 方法=").append(requestMethod)
                .append(", 参数=").append(requestParams)
                .append("\n");

        List<MockConfig> mockConfigs = mockData.getMocks();
        for (int i = 0; i < mockConfigs.size(); i++) {
            MockConfig config = mockConfigs.get(i);
            MockRequest request = config.getRequest();

            boolean pathMatched = isPathMatch(config, requestPath);
            boolean methodMatched = isMethodMatched(request, requestMethod);
            int matchedParams = request.getParams() != null ?
                    calculateMatchedParams(request.getParams(), requestParams) : 0;
            int priority = calculatePriority(config, requestMethod, requestParams);

            details.append("配置项[").append(i + 1).append("]: ")
                    .append("路径=").append(request.getPath())
                    .append("(匹配:").append(pathMatched).append("), ")
                    .append("方法=").append(request.getMethod())
                    .append("(匹配:").append(methodMatched).append("), ")
                    .append("参数=").append(request.getParams())
                    .append("(匹配数:").append(matchedParams).append("), ")
                    .append("优先级=").append(priority)
                    .append("\n");
        }

        MockConfig bestMatch = findBestMatch(requestPath, requestMethod, requestParams, mockData);
        details.append("最佳匹配: ").append(bestMatch != null ? "找到" : "未找到");

        return details.toString();
    }
}
