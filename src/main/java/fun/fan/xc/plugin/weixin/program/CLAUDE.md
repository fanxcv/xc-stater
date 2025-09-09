# 微信小程序模块 (fun.fan.xc.plugin.weixin.program)

## 模块职责

该模块提供了微信小程序相关的API接口和管理功能，包括Access Token管理、API调用、登录验证等。

## 核心组件

### API接口类
- `ProgramWeiXinApi`: 小程序API实现类，继承自BaseWeiXinApi
- `ProgramWeiXinApiClient`: 小程序API客户端接口

### 控制器类
- `ProgramWeiXinApiController`: 小程序API控制器，处理HTTP请求

### Token管理类
- `ProgramAccessTokenManager`: 小程序Access Token管理器

## 关键文件

### API实现类
- `ProgramWeiXinApi.kt`: 小程序API实现类，提供了以下功能：
  - 小程序登录(code2Session)
  - 获取Access Token
  - 发送订阅消息
  - 生成小程序码
  - 微信支付相关接口

### API客户端接口
- `ProgramWeiXinApiClient.kt`: 小程序API客户端接口，定义了与微信服务器交互的方法

### 控制器类
- `ProgramWeiXinApiController.kt`: 小程序API控制器，处理以下HTTP请求：
  - 小程序消息接收和处理
  - 支付通知回调

### Token管理类
- `ProgramAccessTokenManager.kt`: 小程序Access Token管理器，负责：
  - Access Token的获取和刷新
  - 继承自WeiXinBaseTokenManager
  - 实现doRefresh方法调用requestToken

## 接口规范

### API调用规范
1. 继承BaseWeiXinApi基类，复用通用功能
2. 使用NetUtils工具类进行HTTP请求
3. 提供完整的错误处理机制
4. 遵循RESTful API设计规范

### Token管理规范
1. 实现WeiXinBaseTokenManager抽象类
2. 提供具体的doRefresh实现
3. 使用WeiXinTokenRequest获取Token
4. 支持自动刷新机制

## 依赖关系

### 外部依赖
- `Spring Boot`: Spring框架
- `FastJSON2`: JSON处理
- `Hutool`: 工具类库
- `wechatpay-java`: 微信支付Java SDK

### 内部依赖
- `fun.fan.xc.plugin.weixin`: 微信插件包
- `fun.fan.xc.plugin.weixin.BaseWeiXinApi`: 微信API基类
- `fun.fan.xc.plugin.weixin.token`: Token管理模块
- `fun.fan.xc.plugin.weixin.entity`: 微信实体类模块
- `fun.fan.xc.starter.utils`: 工具类
- `fun.fan.xc.starter.exception`: 异常处理

## 测试要点

1. API接口的功能测试
2. Token管理的并发安全性测试
3. 小程序登录流程测试
4. 消息处理的正确性测试
5. 支付接口的沙箱测试

## 编码规范

1. 使用Kotlin编写业务逻辑
2. 遵循Spring Boot的注解规范
3. 提供完整的日志记录
4. 实现统一的异常处理
5. 使用Lombok简化实体类代码