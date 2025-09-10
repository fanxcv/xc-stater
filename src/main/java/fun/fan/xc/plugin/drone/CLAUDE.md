# Drone CI/CD集成模块 (drone)

## 模块职责

Drone CI/CD集成模块提供了与Drone持续集成和持续部署平台的集成功能。该模块封装了与Drone API的交互，简化了在项目中集成Drone相关功能的复杂性。

## 核心组件

### 1. 启动注解
- `@EnableDroneApi`: 启用Drone API功能的注解

### 2. 核心类
- `DroneRepoService`: Drone仓库服务类，提供仓库相关操作
- `DroneBuildService`: Drone构建服务类，提供构建相关操作

## 核心功能

### 1. Drone平台集成
- 提供了与Drone CI/CD平台的API集成
- 支持仓库和构建相关操作

## 使用方式

1. 在Spring Boot启动类上添加`@EnableDroneApi`注解

## 依赖模块

- 无特定依赖模块