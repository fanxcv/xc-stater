# 认证授权模块 (auth)

## 模块职责

认证授权模块提供了基于 Token 的认证机制和权限控制功能。该模块支持自定义用户认证接口，可以灵活地集成到不同的业务场景中。

## 核心组件

### 1. 注解类
- `@AuthUser`: 用于参数注入已登录用户信息
- `@AuthIgnore`: 用于标识忽略认证检查的接口
- `@AuthPermission`: 用于标识需要特定权限的接口

### 2. 拦截器
- `BaseAuthInterceptor`: 基础认证拦截器，负责处理认证和权限检查逻辑

### 3. 参数解析器
- `UserHandlerMethodArgumentResolver`: 用户对象参数解析器，用于自动注入当前认证用户对象

### 4. 核心接口
- `XcAuthInterface`: 认证接口，需要业务方实现该接口以提供用户认证逻辑
- `XcBaseUser`: 用户基础接口，定义了用户对象的基本属性和方法

### 5. 工具类
- `AuthUtil`: 认证工具类，提供了 Token 管理等相关功能
- `AuthLocal`: 本地线程变量工具类，用于存储当前认证用户信息

## 使用方式

1. 实现 `XcAuthInterface` 接口，提供用户认证逻辑
2. 在 Spring Boot 启动类上添加 `@EnableAuth` 注解
3. 在需要获取当前用户信息的方法参数中使用 `@AuthUser` 注解
4. 在需要权限控制的接口上添加 `@AuthPermission` 注解

## 配置参数

- `xc.authentication`: 认证相关配置，包括 Token 名称、过期时间等

## 依赖模块

- `fun.fan.xc.plugin.redis`: 用于 Token 的存储和管理