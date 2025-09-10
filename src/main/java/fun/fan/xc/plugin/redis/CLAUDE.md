# Redis 扩展模块 (redis)

## 模块职责

Redis 扩展模块提供了 Redis 相关的工具类和配置，简化了 Redis 在项目中的使用。该模块封装了 Redis 的常用操作，提供了更便捷的 API。

## 核心组件

### 1. 启动注解
- `@EnableRedis`: 启用 Redis 功能的注解

### 2. 配置类
- `RedisConfigure`: Redis 配置类，自动配置 RedisTemplate

### 3. 核心类
- `SpringRedisImpl`: Redis 操作实现类，实现了 Redis 接口
- `Redis`: Redis 操作接口，定义了 Redis 的常用操作方法

### 4. RedisTemplate
- `xcRedisTemplate`: 用于常规 Redis 操作的模板
- `xcRedisSubscriber`: 用于 Redis 消息订阅的模板

## 核心功能

### 1. Redis 操作封装
- 提供了常用的 Redis 操作方法，如 set、get、delete 等
- 支持字符串、哈希、列表、集合等数据结构操作
- 支持对象的序列化和反序列化
- 提供了分布式锁功能
- 支持 Redis 脚本执行
- 支持消息发布/订阅

### 2. 自动配置
- 自动配置 Jackson2JsonRedisSerializer 和 FastJsonRedisSerializer
- 提供两种不同的 RedisTemplate 实例

## 使用方式

1. 在 Spring Boot 启动类上添加 `@EnableRedis` 注解
2. 通过依赖注入使用 `Redis` 接口进行 Redis 操作

## 依赖模块

- Spring Data Redis