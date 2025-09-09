# 网关模块 (fun.fan.xc.plugin.gateway)

## 模块职责

该模块提供了API网关功能，包括API分组管理、权限校验、IP黑白名单检查等。

## 核心组件

### 核心类
- `XcGatewayHandler`: 网关处理器接口
- `DefaultGatewayHandler`: 默认网关处理器实现
- `ApiGroup`: API分组注解
- `ApiCheck`: API检查注解

### 责任链类
- `AbstractGatewayChain`: 网关责任链接口
- `AbstractIpCheckChain`: IP检查责任链基类
- `IpWhiteListCheckChain`: IP白名单检查链
- `IpBlackListCheckChain`: IP黑名单检查链

## 关键文件

### 核心类
- `XcGatewayHandler.kt`: 网关处理器接口，定义了网关处理方法
- `DefaultGatewayHandler.kt`: 默认网关处理器实现，提供默认的网关处理逻辑
- `ApiGroup.kt`: API分组注解，用于标记API分组
- `ApiCheck.kt`: API检查注解，用于标记需要检查的API

### 责任链类
- `AbstractGatewayChain.kt`: 网关责任链接口，定义了责任链的基本方法
- `AbstractIpCheckChain.kt`: IP检查责任链基类，提供IP检查的通用逻辑
- `IpWhiteListCheckChain.kt`: IP白名单检查链，实现白名单检查逻辑
- `IpBlackListCheckChain.kt`: IP黑名单检查链，实现黑名单检查逻辑

## 接口规范

### 网关处理规范
1. 实现XcGatewayHandler接口
2. 提供完整的API分组管理
3. 支持注解方式配置
4. 实现责任链模式处理

### 责任链规范
1. 继承AbstractGatewayChain基类
2. 实现具体的处理逻辑
3. 支持链式调用
4. 提供灵活的扩展机制

## 依赖关系

### 外部依赖
- `Spring Boot`: Spring框架
- `FastJSON2`: JSON处理

### 内部依赖
- `fun.fan.xc.starter`: 核心启动模块
- `fun.fan.xc.starter.utils`: 工具类
- `fun.fan.xc.starter.exception`: 异常处理

## 测试要点

1. 网关处理器的功能测试
2. API分组管理的正确性测试
3. IP黑白名单检查的准确性测试
4. 责任链模式的链式调用测试

## 编码规范

1. 使用Kotlin编写业务逻辑
2. 遵循Spring Boot的注解规范
3. 提供完整的日志记录
4. 实现统一的异常处理
5. 使用责任链模式优化处理流程