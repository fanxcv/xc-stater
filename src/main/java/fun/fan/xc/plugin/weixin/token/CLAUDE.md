# 微信Token管理模块 (fun.fan.xc.plugin.weixin.token)

## 模块职责

该模块负责微信各种Token的管理，包括Access Token、JS Ticket等，提供了本地和分布式两种存储方式，并实现了自动刷新机制。

## 核心组件

### Token管理接口
- `WeiXinTokenManager`: Token管理接口，定义了Token的基本操作方法
- `WeiXinBaseTokenManager`: Token管理基类，实现了Token的自动刷新机制

### Token请求接口
- `WeiXinTokenRequest`: Token请求接口，定义了获取Token的方法
- `WeiXinLocalTokenRequest`: 本地Token请求实现，适用于单机部署
- `WeiXinRedisTokenRequest`: Redis分布式Token请求实现，适用于分布式部署

### Token实体类
- `WeiXinTokenManager.BaseTokenEntity`: Token基础实体类
- `WeiXinBaseTokenManager.TokenEntity`: Token实体类，继承自BaseTokenEntity

## 关键文件

### Token管理接口
- `WeiXinTokenManager.kt`: Token管理接口，定义了以下方法：
  - `key()`: 获取唯一标识
  - `token()`: 获取Token
  - `expires()`: 获取Token到期时间
  - `init()`: 初始化方法
  - `refresh()`: 刷新Token

### Token管理基类
- `WeiXinBaseTokenManager.kt`: Token管理基类，实现了以下功能：
  - 线程安全的Token刷新机制（使用ReentrantLock和AtomicInteger）
  - 自动刷新策略（同步刷新和异步刷新）
  - Token有效性检查
  - Spring InitializingBean接口实现

### Token请求接口
- `WeiXinTokenRequest.kt`: Token请求接口，定义了fetchToken方法

### 本地Token请求实现
- `WeiXinLocalTokenRequest.kt`: 本地Token请求实现，直接调用WeiXinUtils解析Token

### Redis分布式Token请求实现
- `WeiXinRedisTokenRequest.kt`: Redis分布式Token请求实现，提供了以下功能：
  - Redis缓存检查
  - 分布式锁机制
  - Redis缓存更新
  - Token解析和存储

## 接口规范

### Token管理机制
1. 使用 `ReentrantLock` 和 `AtomicInteger` 实现了线程安全的Token刷新机制
2. 支持本地模式和Redis分布式模式
3. 采用异步刷新策略，在Token过期前自动刷新
4. 提供Token有效性检查和自动更新功能

### 刷新策略
1. 如果Token未初始化或已过期，同步刷新
2. 如果Token在有效期内但需要刷新，异步刷新
3. 使用原子计数器防止重复刷新

## 依赖关系

### 外部依赖
- `Redis`: 分布式缓存
- `Spring Boot`: Spring框架
- `Kotlin Coroutines`: 协程支持

### 内部依赖
- `fun.fan.xc.plugin.weixin`: 微信插件包
- `fun.fan.xc.plugin.weixin.WeiXinUtils`: 微信工具类
- `fun.fan.xc.starter.exception`: 异常处理

## 测试要点

1. Token管理机制的并发安全性测试
2. 同步刷新和异步刷新的正确性测试
3. Redis分布式模式的功能测试
4. Token过期和刷新的边界条件测试
5. 异常情况下的容错测试

## 编码规范

1. 使用Kotlin编写业务逻辑
2. 遵循Spring Boot的注解规范
3. 提供完整的日志记录
4. 实现线程安全的并发控制
5. 使用协程优化异步操作