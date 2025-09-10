# 百度云服务集成模块 (baidu)

## 模块职责

百度云服务集成模块提供了与百度云服务的集成功能，当前主要支持身份证OCR识别功能。该模块封装了百度云API的调用细节，包括Token的自动获取和刷新机制。

## 核心组件

### 1. 配置类
- `BaiduConfig`: 百度云配置类，管理百度云服务的配置参数

### 2. 启动注解
- `@EnableBaiduApi`: 启用百度云API功能的注解

### 3. 核心类
- `BaiduCloudClient`: 百度云客户端，提供具体API调用方法
- `BaiduCloudTokenManager`: 百度云Token管理器，负责Token的获取和刷新

### 4. 实体类
- `OcrIdCardBody`: 身份证OCR识别请求体
- `OcrIdCardResponse`: 身份证OCR识别响应体
- `BaseResponse`: 响应基类

## 核心功能

### 1. 身份证OCR识别
- 支持身份证正面和反面的识别
- 支持检测身份证被PS、翻拍等风险类型
- 支持检测身份证质量（清晰度、完整性等）
- 支持检测头像内容和身份证裁剪
- 支持检测身份证图片方向

### 2. Token自动管理
- 自动获取百度云Access Token
- 异步刷新Token机制，确保Token的有效性
- 支持分布式环境下的Token同步

## 使用方式

1. 在Spring Boot启动类上添加`@EnableBaiduApi`注解
2. 在`application.yml`或`application.properties`中配置百度云相关参数
3. 通过依赖注入使用`BaiduCloudClient`进行API调用

## 配置参数

在`application.yml`中添加以下配置：

```yaml
xc:
  baidu:
    cloud:
      enable: true # 是否启用百度云功能
      client-id: your_client_id # 百度云应用的API Key
      client-secret: your_client_secret # 百度云应用的Secret Key
```

## 依赖模块

- 无特定依赖模块，但需要百度云相关服务的API Key和Secret Key