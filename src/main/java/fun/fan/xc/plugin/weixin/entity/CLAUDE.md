# 微信实体类模块 (fun.fan.xc.plugin.weixin.entity)

## 模块职责

该模块包含了微信开发中使用的所有数据实体类和枚举类，用于封装微信API的请求和响应数据。

## 核心组件

### 响应实体类
- `WXBaseResp`: 微信基础响应类
- `WXAccessTokenCommonResp`: 微信Access Token通用响应类
- `WXLoginResp`: 微信登录响应类
- `ProgramLoginResp`: 小程序登录响应类
- `WXUserInfo`: 微信用户信息类
- `TemplateResp`: 模板响应类
- `MediaUploadResp`: 媒体上传响应类
- `QrCodeCreateResp`: 二维码创建响应类

### 支付相关实体类
- `PayBase`: 支付基础类
- `PayBaseResp`: 支付基础响应类
- `PayOrder`: 支付订单类
- `PayUnifiedOrder`: 统一下单类
- `PayUnifiedOrderResp`: 统一下单响应类
- `PayRefund`: 退款类
- `PayRefundResp`: 退款响应类
- `PayRefundNotifyResp`: 退款通知响应类
- `PayContractOrder`: 合约订单类
- `PayContractOrderResp`: 合约订单响应类
- `PayDeleteContract`: 删除合约类
- `PayDeleteContractResp`: 删除合约响应类
- `PayPapPayApply`: 委托代扣申请类
- `PayPapPayApplyNotifyResp`: 委托代扣申请通知响应类
- `PayAddOrDelContractNotifyResp`: 添加或删除合约通知响应类
- `PayPreEntrustWeb`: 预委托Web类
- `PayNotifyResp`: 支付通知响应类

### 消息相关实体类
- `MessageItem`: 消息项类
- `TextMessage`: 文本消息类
- `ImageMessage`: 图片消息类
- `TemplateMessage`: 模板消息类
- `SubscribeMessage`: 订阅消息类
- `CustomMessage`: 客服消息类

### 菜单和二维码实体类
- `Menu`: 菜单类
- `QrCodeCreate`: 二维码创建类
- `MPUnlimitedQRCode`: 小程序无限二维码类

### 枚举类
- `WXType`: 微信类型枚举
- `LangEnum`: 语言枚举
- `ErrorCodeEnum`: 错误码枚举
- `SignType`: 签名类型枚举
- `RefundStatus`: 退款状态枚举
- `RefundRequestSource`: 退款请求来源枚举
- `RefundAccount`: 退款账户枚举
- `ActionNameEnum`: 动作名称枚举
- `MPVersionEnum`: 小程序版本枚举
- `MiniProgramStateEnum`: 小程序状态枚举
- `TradeType`: 交易类型枚举

## 关键文件

### 响应实体类
- `WXBaseResp.kt`: 微信基础响应类
- `WXAccessTokenCommonResp.kt`: 微信Access Token通用响应类
- `WXLoginResp.kt`: 微信登录响应类
- `ProgramLoginResp.kt`: 小程序登录响应类
- `WXUserInfo.kt`: 微信用户信息类
- `TemplateResp.kt`: 模板响应类
- `MediaUploadResp.kt`: 媒体上传响应类
- `QrCodeCreateResp.kt`: 二维码创建响应类

### 支付相关实体类
- `PayBase.kt`: 支付基础类
- `PayBaseResp.kt`: 支付基础响应类
- `PayOrder.kt`: 支付订单类
- `PayUnifiedOrder.kt`: 统一下单类
- `PayUnifiedOrderResp.kt`: 统一下单响应类
- `PayRefund.kt`: 退款类
- `PayRefundResp.kt`: 退款响应类
- `PayRefundNotifyResp.kt`: 退款通知响应类
- `PayContractOrder.kt`: 合约订单类
- `PayContractOrderResp.kt`: 合约订单响应类
- `PayDeleteContract.kt`: 删除合约类
- `PayDeleteContractResp.kt`: 删除合约响应类
- `PayPapPayApply.kt`: 委托代扣申请类
- `PayPapPayApplyNotifyResp.kt`: 委托代扣申请通知响应类
- `PayAddOrDelContractNotifyResp.kt`: 添加或删除合约通知响应类
- `PayPreEntrustWeb.kt`: 预委托Web类
- `PayNotifyResp.kt`: 支付通知响应类

### 消息相关实体类
- `MessageItem.kt`: 消息项类
- `TextMessage.kt`: 文本消息类
- `ImageMessage.kt`: 图片消息类
- `TemplateMessage.kt`: 模板消息类
- `SubscribeMessage.kt`: 订阅消息类
- `CustomMessage.kt`: 客服消息类

### 菜单和二维码实体类
- `Menu.kt`: 菜单类
- `QrCodeCreate.kt`: 二维码创建类
- `MPUnlimitedQRCode.kt`: 小程序无限二维码类

### 枚举类
- `WXType.kt`: 微信类型枚举
- `LangEnum.kt`: 语言枚举
- `ErrorCodeEnum.kt`: 错误码枚举
- `SignType.kt`: 签名类型枚举
- `RefundStatus.kt`: 退款状态枚举
- `RefundRequestSource.kt`: 退款请求来源枚举
- `RefundAccount.kt`: 退款账户枚举
- `ActionNameEnum.kt`: 动作名称枚举
- `MPVersionEnum.kt`: 小程序版本枚举
- `MiniProgramStateEnum.kt`: 小程序状态枚举
- `TradeType.kt`: 交易类型枚举

## 接口规范

所有实体类都遵循以下规范：
1. 使用Lombok注解简化代码
2. 实现序列化接口以便于网络传输
3. 提供清晰的字段注释说明
4. 遵循Java命名规范

## 依赖关系

### 外部依赖
- `Lombok`: 用于简化Java代码
- `FastJSON2`: 用于JSON序列化和反序列化

### 内部依赖
- `fun.fan.xc.starter.utils`: 工具类
- `fun.fan.xc.plugin.weixin`: 微信插件包

## 测试要点

1. 所有实体类的序列化/反序列化测试
2. 枚举类的值映射测试
3. 支付相关实体类的签名验证测试
4. 时间字段的格式化测试

## 编码规范

1. 使用Lombok注解减少样板代码
2. 遵循JavaBean规范
3. 提供完整的字段注释
4. 枚举类提供清晰的含义说明