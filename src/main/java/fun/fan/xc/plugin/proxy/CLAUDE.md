# 代理服务模块 (proxy)

## 模块职责

代理服务模块提供了HTTP代理功能，基于Netty实现，支持负载均衡、连接池管理、请求转发等功能。该模块能够将接收到的HTTP请求转发到配置的目标服务器，并将响应返回给客户端。

## 核心组件

### 1. 启动注解
- `@EnableXcProxy`: 启用代理功能的注解

### 2. 配置类
- `ProxyProperties`: 代理配置属性类，定义了代理相关的配置参数
- `ProxyRoute`: 路由配置类，定义代理路由配置
- `ProxyDestination`: 目标配置类，定义代理目标配置
- `ProxyConfigurationManager`: 配置管理器，负责代理配置的管理和查询

### 3. 核心处理类
- `ProxyInterceptor`: 代理拦截器，专注于拦截Spring MVC请求，判断是否需要代理
- `ProxyOrchestrator`: 代理协调器，负责协调整个代理请求的流程
- `ProxyLoadBalancer`: 负载均衡器，支持权重轮询算法和失败重试机制

### 4. 客户端组件
- `ProxyClient`: 代理客户端门面类，提供简单的代理请求接口
- `HostPortChannelPool`: Host:Port连接池，专注于Netty连接的池化管理和生命周期控制

### 5. 转换器组件
- `HttpRequestTransformer`: HTTP请求转换器，负责将Servlet请求转换为Netty HTTP请求
- `HttpResponseTransformer`: HTTP响应转换器，负责将Netty HTTP响应转换为Servlet响应

### 6. 异常处理
- `ProxyException`: 代理异常类，用于处理代理过程中的异常情况

## 核心功能

### 1. HTTP代理
- 支持将HTTP请求转发到目标服务器
- 完整的请求参数透传、消息体透传、Header透传
- 支持multipart请求的处理和重构

### 2. 负载均衡
- 支持基于权重的轮询负载均衡算法
- 支持失败重试机制，提高系统可用性
- 支持平滑加权轮询算法，确保权重分配准确性

### 3. 连接池管理
- 基于Netty的连接池管理，提高连接复用率
- 支持按host:port分组管理连接池
- 连接的异步获取和释放

### 4. 配置化路由
- 支持通过配置文件定义路由规则
- 支持多个目标地址的配置，支持权重分配
- **支持独立配置HTTP聚合器大小**，可为不同路由和目标配置不同的聚合器大小，优化性能和资源使用

### 5. 请求/响应转换
- 支持请求和响应的转换与透传
- 智能处理各种HTTP Headers
- 支持各种响应类型的处理

## 使用方式

1. 在Spring Boot应用的主类上添加`@EnableXcProxy`注解
2. 在`application.yml`或`application.properties`中配置路由规则
3. 通过依赖注入使用相关组件

### 配置示例

```yaml
xc:
  proxy:
    timeout: 30000
    max-connections: 10
    max-wait-queue-size: 20
    max-wait-timeout: 10000
    route:
      # 路由级别聚合器大小配置
      - source: /api/fileupload/**
        maxAggregatorSize: 104857600  # 100MB，用于大文件上传
        target:
          - uri: http://server1.example.com
            weight: 5
          - uri: http://server2.example.com
            weight: 3
      # 目标级别聚合器大小配置（优先级更高）
      - source: /api/normal/**
        target:
          - uri: http://server3.example.com
            weight: 3
            maxAggregatorSize: 8388608  # 8MB，优先使用此配置
          - uri: http://server4.example.com
            weight: 1
            # 使用路由级别的聚合器大小配置 (64MB)
      # 默认配置（32MB）
      - source: /api/default/**
        target:
          - uri: http://server5.example.com
            weight: 1
```

### 聚合器大小配置说明

HTTP对象聚合器（HttpObjectAggregator）用于控制HTTP请求/响应消息体的最大聚合大小。

**配置层级和优先级：**
1. **目标级配置**：`target[].maxAggregatorSize` - 最高优先级
2. **路由级配置**：`route[].maxAggregatorSize` - 中优先级
3. **默认值**：33554432 字节 (32MB) - 最低优先级

**使用场景：**
- **大文件上传/下载**：设置更大的聚合器大小（如 100MB、200MB）
- **普通API调用**：使用较小的聚合器大小（如 8MB、16MB）以节省资源
- **微消息体**：可设置更小的聚合器大小（如 1MB）以提高性能

**注意事项：**
- 聚合器大小设置过小会导致大请求/响应被拒绝
- 聚合器大小设置过大可能会消耗过多内存
- 建议根据实际业务场景合理配置

## 配置参数

### 全局配置

| 参数 | 说明 | 默认值 | 取值范围 |
| :--- | :--- | :--- | :--- |
| xc.proxy.timeout | 超时时间(毫秒) | 30000 | - |
| xc.proxy.max-connections | 总连接数 | 10 | 1-100 |
| xc.proxy.max-wait-queue-size | 等待队列长度 | 20 | 1-1000 |
| xc.proxy.max-wait-timeout | 等待时间(毫秒) | 10000 | 1000-60000 |
| xc.proxy.route | 路由配置列表 | null | - |

### 路由配置

| 参数 | 说明 | 默认值 | 取值范围 |
| :--- | :--- | :--- | :--- |
| route[].source | 源接口地址 | - | - |
| route[].timeout | 路由级超时时间(毫秒) | null | - |
| route[].retryCount | 路由级重试次数 | null | 0-10 |
| route[].maxAggregatorSize | 路由级聚合器大小(字节) | null (使用默认值32MB) | 1MB-200MB |

### 目标配置

| 参数 | 说明 | 默认值 | 取值范围 |
| :--- | :--- | :--- | :--- |
| target[].uri | 目标URI地址 | - | - |
| target[].weight | 权重 | 1 | 1-100 |
| target[].maxAggregatorSize | 目标级聚合器大小(字节) | null (使用路由级配置) | 1MB-200MB |

### 聚合器大小配置说明

**优先级顺序：目标级 > 路由级 > 默认值**

- **默认值**：33554432 字节 (32MB)
- **建议范围**：
  - 微消息体：1MB - 8MB
  - 普通API：8MB - 32MB
  - 大文件场景：64MB - 200MB

## 依赖模块

- Netty
- Kotlin Coroutines
- Spring Boot Web