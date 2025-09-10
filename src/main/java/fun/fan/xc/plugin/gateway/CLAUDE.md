# 网关路由模块 (gateway)

## 模块职责

网关路由模块提供了可扩展的网关链式处理机制，支持通过责任链模式对请求进行处理。该模块可以用于实现 IP 黑白名单检查、请求过滤、权限校验等功能。

## 核心组件

### 1. 启动注解
- `@EnableXcGateway`: 启用网关功能的注解

### 2. 核心类
- `AbstractGatewayChain`: 网关链抽象基类，定义了链式处理的基本结构和方法
- `DefaultGatewayHandler`: 默认网关处理器，自动配置IP黑白名单检查链

### 3. 链式处理实现类
- `AbstractIpCheckChain`: IP 检查链抽象类
- `IpBlackListCheckChain`: IP 黑名单检查链
- `IpWhiteListCheckChain`: IP 白名单检查链

## 核心功能

### 1. 链式处理机制
- 支持通过责任链模式对请求进行处理
- 可以动态添加或移除处理链
- 支持自定义处理逻辑

### 2. IP 黑白名单检查
- 提供了 IP 黑名单检查功能
- 提供了 IP 白名单检查功能

### 3. 默认处理链
- 默认配置了IP黑名单检查链和IP白名单检查链
- 处理链顺序：IP黑名单校验 -> IP白名单校验

## 使用方式

1. 在 Spring Boot 启动类上添加 `@EnableXcGateway` 注解
2. 继承 `AbstractGatewayChain` 类实现自定义处理逻辑
3. 通过 `AbstractGatewayChain.builder()` 构建处理链
4. 调用 `exec()` 方法执行链式处理

## 依赖模块

- 无特定依赖模块