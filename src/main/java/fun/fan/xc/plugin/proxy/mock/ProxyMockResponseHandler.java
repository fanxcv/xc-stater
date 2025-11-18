package fun.fan.xc.plugin.proxy.mock;

import fun.fan.xc.plugin.proxy.mock.dto.MockConfig;
import fun.fan.xc.plugin.proxy.mock.dto.MockRequest;
import fun.fan.xc.plugin.proxy.mock.dto.MockResponse;
import fun.fan.xc.starter.exception.XcServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Proxy Mock响应处理器
 * 负责将Mock配置转换为实际的HTTP响应
 *
 * @author fan
 */
@Slf4j
public class ProxyMockResponseHandler {

    /**
     * 处理Mock响应
     *
     * @param mockConfig Mock配置
     * @param request    原始HTTP请求
     * @param response   HTTP响应对象
     * @throws IOException 如果响应写入失败
     */
    public void handleMockResponse(MockConfig mockConfig, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (mockConfig == null || mockConfig.getResponse() == null) {
            throw new XcServiceException("Mock配置或响应配置为空");
        }

        MockResponse mockResponse = mockConfig.getResponse();

        log.debug("处理Mock响应: 状态码={}, Headers数量={}, Body长度={}",
                mockResponse.getStatus(),
                mockResponse.getHeaders() != null ? mockResponse.getHeaders().size() : 0,
                mockResponse.getBody() != null ? mockResponse.getBody().length() : 0);

        // 设置响应状态码
        response.setStatus(mockResponse.getStatus() != null ? mockResponse.getStatus() : 200);

        // 设置响应头
        setResponseHeaders(mockResponse.getHeaders(), response);

        // 设置响应体
        setResponseBody(mockResponse.getBody(), response);

        log.info("已返回Mock响应: 请求路径={}, 状态码={}", request.getRequestURI(), response.getStatus());
    }

    /**
     * 设置响应头
     */
    private void setResponseHeaders(Map<String, String> headers, HttpServletResponse response) {
        if (CollectionUtils.isEmpty(headers)) {
            return;
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            String headerName = header.getKey();
            String headerValue = header.getValue();

            if (headerName != null && headerValue != null) {
                // 过滤掉一些可能引起问题的响应头
                if (shouldSkipHeader(headerName)) {
                    log.debug("跳过设置响应头: {}={}", headerName, headerValue);
                    continue;
                }

                response.setHeader(headerName, headerValue);
                log.debug("设置响应头: {}={}", headerName, headerValue);
            }
        }
    }

    /**
     * 判断是否应该跳过设置某个响应头
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        // 跳过一些由Servlet容器管理的响应头
        return lowerName.equals("content-length") ||
                lowerName.equals("connection") ||
                lowerName.equals("transfer-encoding") ||
                lowerName.equals("server");
    }

    /**
     * 设置响应体
     */
    private void setResponseBody(String body, HttpServletResponse response) throws IOException {
        if (body == null) {
            body = "";
        }

        // 设置Content-Type（如果没有设置的话）
        if (response.getContentType() == null) {
            String contentType = guessContentType(body);
            response.setContentType(contentType);
            log.debug("自动设置Content-Type: {}", contentType);
        }

        // 设置Content-Length
        String encoding = StandardCharsets.UTF_8.name();
        response.setContentLength(body.getBytes(encoding).length);
        response.setCharacterEncoding(encoding);

        // 写入响应体
        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
            writer.flush();
        }

        log.debug("已写入响应体，长度: {}", body.length());
    }

    /**
     * 根据响应内容猜测Content-Type
     */
    private String guessContentType(String body) {
        if (body.trim().isEmpty()) {
            return "text/plain";
        }

        String trimmedBody = body.trim();

        // JSON格式检测
        if ((trimmedBody.startsWith("{") && trimmedBody.endsWith("}")) ||
                (trimmedBody.startsWith("[") && trimmedBody.endsWith("]"))) {
            return "application/json; charset=utf-8";
        }

        // XML格式检测
        if (trimmedBody.startsWith("<?xml") || trimmedBody.startsWith("<")) {
            return "application/xml; charset=utf-8";
        }

        // HTML格式检测
        if (trimmedBody.startsWith("<html") || trimmedBody.startsWith("<!DOCTYPE")) {
            return "text/html; charset=utf-8";
        }

        // 默认为纯文本
        return "text/plain; charset=utf-8";
    }

    /**
     * 验证Mock配置的有效性
     */
    public boolean validateMockConfig(MockConfig mockConfig) {
        if (mockConfig == null) {
            log.warn("Mock配置为null");
            return false;
        }

        if (mockConfig.getRequest() == null) {
            log.warn("Mock请求配置为null");
            return false;
        }

        if (mockConfig.getResponse() == null) {
            log.warn("Mock响应配置为null");
            return false;
        }

        MockRequest request = mockConfig.getRequest();
        if (!org.springframework.util.StringUtils.hasText(request.getPath())) {
            log.warn("Mock请求路径为空");
            return false;
        }

        MockResponse response = mockConfig.getResponse();
        if (response.getStatus() != null && (response.getStatus() < 100 || response.getStatus() > 599)) {
            log.warn("无效的HTTP状态码: {}", response.getStatus());
            return false;
        }

        return true;
    }
}
