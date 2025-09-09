# 微信插件模块 (fun.fan.xc.plugin.weixin)

## 模块职责

该模块提供了完整的微信公众号和小程序开发支持，包括认证授权、支付、消息管理、素材管理等功能。

## 核心组件

### 配置类
- `WeiXinConfig`: 微信配置类，管理公众号、小程序、服务端、客户端的配置
- `WeiXinConfig.Official`: 公众号配置
- `WeiXinConfig.MiniProgram`: 小程序配置
- `WeiXinConfig.Server`: 服务端配置
- `WeiXinConfig.Client`: 客户端配置
- `WeiXinConfig.Pay`: 支付配置

### 基础API类
- `BaseWeiXinApi`: 微信API基类，提供通用的Token获取和支付配置方法
- `WeiXinApiClient`: 微信API客户端接口

### Token管理
- `WeiXinTokenManager`: Token管理接口
- `WeiXinBaseTokenManager`: Token管理基类，实现了Token的自动刷新机制
- `WeiXinTokenRequest`: Token请求接口
- `WeiXinLocalTokenRequest`: 本地Token请求实现
- `WeiXinRedisTokenRequest`: Redis分布式Token请求实现

### 公众号相关
- `OfficialWeiXinApi`: 公众号API实现类
- `OfficialWeiXinApiClient`: 公众号API客户端
- `OfficialAccessTokenManager`: 公众号Access Token管理器
- `OfficialJsApiTicketManager`: 公众号JS Ticket管理器

### 小程序相关
- `ProgramWeiXinApi`: 小程序API实现类
- `ProgramWeiXinApiClient`: 小程序API客户端
- `ProgramAccessTokenManager`: 小程序Access Token管理器

## 关键文件

### 配置文件
- `WeiXinConfig.java`: 微信配置类，定义了所有微信相关的配置项

### 核心API类
- `BaseWeiXinApi.kt`: 微信API基类，提供支付配置和Token获取功能
- `WeiXinApiClient.kt`: 微信API客户端接口

### Token管理类
- `WeiXinBaseTokenManager.kt`: Token管理基类，实现了线程安全的Token刷新机制
- `WeiXinTokenManager.kt`: Token管理接口
- `WeiXinTokenRequest.kt`: Token请求接口
- `WeiXinLocalTokenRequest.kt`: 本地Token请求实现
- `WeiXinRedisTokenRequest.kt`: Redis分布式Token请求实现
- `OfficialAccessTokenManager.kt`: 公众号Access Token管理器
- `ProgramAccessTokenManager.kt`: 小程序Access Token管理器
- `OfficialJsApiTicketManager.kt`: 公众号JS Ticket管理器

### 控制器类
- `OfficialWeiXinApiController.kt`: 公众号API控制器
- `ProgramWeiXinApiController.kt`: 小程序API控制器

## 接口规范

### Token管理机制
1. 使用 `ReentrantLock` 和 `AtomicInteger` 实现了线程安全的Token刷新机制
2. 支持本地模式和Redis分布式模式
3. 采用异步刷新策略，在Token过期前自动刷新

### 支付安全
1. 使用SSL Socket Factory处理微信支付的HTTPS请求
2. 支持微信支付V2和V3的签名和验签

## 依赖关系

### 外部依赖
- `wechatpay-java`: 微信支付Java SDK
- `Spring Boot`: Spring框架
- `FastJSON2`: JSON处理
- `Hutool`: 工具类库

### 内部依赖
- `fun.fan.xc.starter`: 核心启动模块
- `fun.fan.xc.starter.utils`: 工具类
- `fun.fan.xc.starter.exception`: 异常处理

## 测试要点

1. Token管理机制的并发安全性测试
2. 支付流程的沙箱测试
3. API调用的模拟测试
4. 各种实体类的序列化/反序列化测试

## 编码规范

1. 主要使用Kotlin编写业务逻辑
2. 使用Lombok简化实体类代码
3. 遵循RESTful API设计规范
4. 使用Spring Boot的注解进行依赖注入和配置