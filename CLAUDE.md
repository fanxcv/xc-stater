# xc-starter 微信开发工具包

## 项目摘要

xc-starter 是一个基于 Spring Boot 的微信开发工具包，提供了对微信公众号和小程序的全面支持。该工具包封装了微信的各种 API 接口，包括但不限于：

### 核心功能
1. **认证与授权**：
   - 公众号 OAuth2 登录
   - 小程序登录 (code2Session)
   - Access Token 和 JS Ticket 管理

2. **支付功能**：
   - 统一下单接口
   - 退款接口
   - 委托代扣（支付中签约、申请扣款、申请解约）
   - 支付结果和退款结果通知处理

3. **消息管理**：
   - 客服消息发送（文本、图片）
   - 模板消息发送
   - 订阅消息发送

4. **素材管理**：
   - 临时素材上传

5. **菜单管理**：
   - 自定义菜单创建

6. **二维码**：
   - 公众号带参数二维码创建
   - 小程序码生成

该工具包设计了灵活的配置机制，支持服务端模式和客户端模式，并提供了基于 Redis 的分布式 Token 管理方案。

## 架构总览

### 模块结构
```
fun.fan.xc.starter
├── plugin.weixin
│   ├── entity      # 微信相关的数据实体类
│   ├── enums       # 微信相关的枚举类
│   ├── token       # 微信 Token 管理相关类
│   ├── official    # 微信公众号相关 API 和管理类
│   └── program     # 微信小程序相关 API 和管理类
└── config          # 核心启动配置和通用工具类
```

### 核心组件
- `WeiXinConfig`：微信配置类，管理公众号、小程序、服务端、客户端的配置
- `BaseWeiXinApi`：微信 API 基类，提供通用的 Token 获取和支付配置方法
- `OfficialWeiXinApi`：公众号 API 实现类
- `ProgramWeiXinApi`：小程序 API 实现类
- `WeiXinBaseTokenManager`：Token 管理基类，实现了 Token 的自动刷新机制
- `OfficialAccessTokenManager`：公众号 Access Token 管理器
- `ProgramAccessTokenManager`：小程序 Access Token 管理器
- `OfficialJsApiTicketManager`：公众号 JS Ticket 管理器

### Token 管理机制
- 使用 `ReentrantLock` 和 `AtomicInteger` 实现了线程安全的 Token 刷新机制
- 支持本地模式（`WeiXinLocalTokenRequest`）和 Redis 分布式模式（`WeiXinRedisTokenRequest`）
- 采用异步刷新策略，在 Token 过期前自动刷新

### 支付安全
- 使用 SSL Socket Factory 处理微信支付的 HTTPS 请求
- 支持微信支付 V2 和 V3 的签名和验签

## 模块索引

| 模块路径 | 职责 |
| :--- | :--- |
| `fun.fan.xc.starter` | 核心启动配置和通用工具类 |
| `fun.fan.xc.plugin.weixin` | 微信插件核心包 |
| `fun.fan.xc.plugin.weixin.entity` | 微信相关的数据实体类 |
| `fun.fan.xc.plugin.weixin.enums` | 微信相关的枚举类 |
| `fun.fan.xc.plugin.weixin.token` | 微信 Token 管理相关类 |
| `fun.fan.xc.plugin.weixin.official` | 微信公众号相关 API 和管理类 |
| `fun.fan.xc.plugin.weixin.program` | 微信小程序相关 API 和管理类 |

## 运行与开发

### 环境要求
- Java 8+
- Kotlin 1.9.23
- Maven 3.x
- Spring Boot 2.7.15

### 依赖管理
- 使用 Maven 进行依赖管理
- 核心依赖包括 Spring Boot Web、FastJSON2、Hutool、Guava 等
- 微信支付依赖 `wechatpay-java`

### 配置方式
- 通过 `application.yml` 或 `application.properties` 配置微信相关参数
- 配置前缀为 `xc.weixin`
- 可分别配置公众号（official）、小程序（mini-program）、服务端（server）、客户端（client）

### 启用方式
- 在 Spring Boot 应用的启动类上添加 `@EnableWeiXinApi` 注解

## 测试策略

从代码分析来看，该项目目前没有包含专门的测试类或测试配置。建议补充以下测试：

### 单元测试
- 对各个实体类进行序列化/反序列化测试
- 对工具类（如签名工具）进行测试

### 集成测试
- 对 API 调用进行模拟测试
- 对 Token 管理机制进行并发测试

### 支付测试
- 对支付流程进行沙箱测试

## 编码规范

### 语言规范
- 主要使用 Kotlin 编写业务逻辑
- 部分注解类使用 Java 编写

### 代码风格
- 使用 Lombok 简化实体类代码
- 使用 Spring Boot 的注解进行依赖注入和配置
- 遵循 RESTful API 设计规范

### 异常处理
- 使用自定义异常类 `XcServiceException` 处理业务异常
- 对微信返回的错误码进行封装和处理

## AI 使用指引

### 代码理解
- 可以利用 AI 快速理解微信各种 API 的调用方式和参数含义
- 可以帮助分析 Token 管理机制的并发安全性

### 代码生成
- 可以根据微信官方文档自动生成新的 API 接口代码
- 可以生成测试用例和测试数据

### 代码优化
- 可以分析现有代码的性能瓶颈并提出优化建议
- 可以帮助重构复杂的业务逻辑

## 变更记录 (Changelog)

- **2025-09-09**：完成对 xc-starter 微信开发工具包的初步分析，梳理了项目结构、核心功能和架构设计。