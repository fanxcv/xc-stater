# 微信开发工具包模块 (weixin)

## 模块职责

微信开发工具包模块提供了对微信公众号和小程序的全面支持，封装了微信的各种 API 接口，包括认证与授权、支付功能、消息管理、素材管理、菜单管理和二维码生成等功能。该模块支持本地和分布式Token管理，提供服务端和客户端两种部署模式。

## 核心组件

### 1. 配置类
- `WeiXinConfig`: 微信配置类，管理公众号、小程序、服务端、客户端的配置

### 2. 启动注解
- `@EnableWeiXinApi`: 启用微信 API 功能的注解，自动导入所有必要的组件

### 3. 核心 API 类
- `BaseWeiXinApi`: 微信 API 基类，提供通用的 Token 获取和支付配置方法
- `OfficialWeiXinApi`: 公众号 API 实现类，包含公众号特有的功能如JS Ticket、菜单管理等
- `ProgramWeiXinApi`: 小程序 API 实现类，包含小程序特有的功能如小程序码生成等

### 4. Token 管理类
- `WeiXinBaseTokenManager`: Token 管理基类，实现了 Token 的自动刷新机制
- `OfficialAccessTokenManager`: 公众号 Access Token 管理器
- `ProgramAccessTokenManager`: 小程序 Access Token 管理器
- `OfficialJsApiTicketManager`: 公众号 JS Ticket 管理器

### 5. 请求处理类
- `WeiXinLocalTokenRequest`: 本地 Token 请求处理类，适用于单机部署
- `WeiXinRedisTokenRequest`: Redis 分布式 Token 请求处理类，适用于集群部署

### 6. 客户端接口类
- `WeiXinApiClient`: 微信客户端接口基类
- `OfficialWeiXinApiClient`: 公众号客户端实现类
- `ProgramWeiXinApiClient`: 小程序客户端实现类

### 7. 控制器类
- `WeiXinApiController`: 微信基础控制器
- `OfficialWeiXinApiController`: 公众号控制器
- `ProgramWeiXinApiController`: 小程序控制器

### 8. 拦截器类
- `WeiXinInterceptor`: 微信服务端请求拦截器，用于身份验证

### 9. 启用类
- `WeixinServerEnable`: 微信服务端启用标记
- `WeixinClientEnable`: 微信客户端启用标记

## 核心功能

### 1. 认证与授权
- 公众号 OAuth2 登录 (`webLoginBase`, `webLoginUserInfo`)
- 小程序登录 (code2Session) (`code2Session`)
- Access Token 和 JS Ticket 管理
- 服务器配置校验 (`checkSignature`)
- JS Ticket 签名 (`jsTicketSign`)

### 2. 支付功能
- 统一下单接口 (`payUnifiedOrder`, `payOrderSimple`)
- 退款接口 (`payRefund`)
- 委托代扣（支付中签约、申请扣款、申请解约）
  - 支付中签约 (`payContractOrder`)
  - 申请扣款 (`payPapPayApply`)
  - 申请解约 (`payDeleteContract`)
- 支付结果和退款结果通知处理
  - 支付回调 (`payNotify`)
  - 退款回调 (`payRefundNotify`)
  - 扣款结果通知 (`payPapPayApplyNotify`)
  - 签约/解约结果通知 (`payAddOrDelContractNotify`)

### 3. 消息管理
- 客服消息发送（文本、图片）(`messageCustomSend`)
- 模板消息发送 (`messageTemplateSend`)
- 订阅消息发送 (`sendMessage`)

### 4. 素材管理
- 临时素材上传 (`mediaUpload`)

### 5. 菜单管理
- 自定义菜单创建 (`createMenu`)

### 6. 二维码
- 公众号带参数二维码创建 (`createQrCode`)
- 小程序码生成 (`getUnlimitedQRCode`)

## 使用方式

1. 在 `application.yml` 或 `application.properties` 中配置微信相关参数，配置前缀为 `xc.weixin`
2. 在 Spring Boot 启动类上添加 `@EnableWeiXinApi` 注解
3. 通过依赖注入使用 `OfficialWeiXinApi` 或 `ProgramWeiXinApi` 进行微信 API 调用

## 配置参数

### 公众号配置
- `xc.weixin.official.enable`: 是否启用公众号接口
- `xc.weixin.official.token`: 微信服务器配置用到的Token
- `xc.weixin.official.appId`: 微信公众号APP ID
- `xc.weixin.official.appSecret`: 微信公众号APP Secret
- `xc.weixin.official.checkTokenWhenStart`: 启动时获取并检查AccessToken

### 小程序配置
- `xc.weixin.miniProgram.enable`: 是否启用小程序接口
- `xc.weixin.miniProgram.appId`: 微信小程序APP ID
- `xc.weixin.miniProgram.appSecret`: 微信小程序APP Secret
- `xc.weixin.miniProgram.checkTokenWhenStart`: 启动时获取并检查AccessToken
- `xc.weixin.miniProgram.pay`: 小程序支付相关配置

### 服务端配置
- `xc.weixin.server.enable`: 是否启用
- `xc.weixin.server.basePath`: 对外暴露接口的基础地址
- `xc.weixin.server.enableAuth`: 是否需要鉴权
- `xc.weixin.server.authentication`: 鉴权用户配置

### 客户端配置
- `xc.weixin.client.enable`: 是否启用
- `xc.weixin.client.server`: Server端请求地址
- `xc.weixin.client.authentication`: 认证信息

### 支付配置
- `xc.weixin.[official|miniProgram].pay.wechatPayCertPath`: V3-微信支付平台证书路径
- `xc.weixin.[official|miniProgram].pay.apiKeySerialNo`: V3-商户证书序列号
- `xc.weixin.[official|miniProgram].pay.apiKeyPath`: V3-商户API私钥路径
- `xc.weixin.[official|miniProgram].pay.apiCertPath`: 微信支付API秘钥地址
- `xc.weixin.[official|miniProgram].pay.apiV2Key`: 微信支付V2Key
- `xc.weixin.[official|miniProgram].pay.apiV3Key`: V3-微信支付V3Key
- `xc.weixin.[official|miniProgram].pay.mchId`: 商户id

## 依赖模块

- `fun.fan.xc.starter.utils`: 工具类模块
- `fun.fan.xc.plugin.redis`: Redis支持模块（用于分布式Token管理）