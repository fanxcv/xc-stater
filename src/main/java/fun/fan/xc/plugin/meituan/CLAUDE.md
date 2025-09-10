# 美团服务集成模块 (meituan)

## 模块职责

美团服务集成模块提供了与美团服务的集成功能。该模块封装了与美团API的交互，简化了在项目中集成美团相关功能的复杂性。

## 核心组件

### 1. 启动注解
- `@EnableMeiTuanApi`: 启用美团API功能的注解

### 2. 核心类
- `MtConfig`: 美团配置类，管理美团服务的配置参数
- `MtTokenService`: 美团Token服务类，提供Token相关操作
- `MtRedisTokenManager`: 美团Redis Token管理器，负责Token的存储和管理

## 核心功能

### 1. 美团服务集成
- 提供了与美团服务的API集成
- 支持Token的自动获取和管理

## 使用方式

1. 在Spring Boot启动类上添加`@EnableMeiTuanApi`注解

## 依赖模块

- 无特定依赖模块