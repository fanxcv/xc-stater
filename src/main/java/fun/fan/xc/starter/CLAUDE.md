# 核心启动模块 (starter)

## 模块职责

核心启动模块是整个 xc-starter 项目的基础，提供了项目启动所需的核心配置和通用工具类。该模块定义了项目的整体结构和基本功能，包括参数校验、异常处理、网络请求、实体转换、加密工具等核心功能。它是整个框架的核心基础设施，为其他功能模块提供基础支撑。

## 核心组件

### 1. 配置类
- `XcConfiguration`: 项目核心配置类，管理项目的整体配置，包括核心配置、认证配置、跨域配置、Drone配置和网关配置
- `XcCoreAutoConfiguration`: 项目核心自动配置类，负责注册核心组件和配置

### 2. 注解类
- `@InjectEntity`: 实体注入注解，用于将请求参数自动注入到实体对象中
- `@VerifyParam`: 参数校验注解，支持正则校验、自定义校验类、参数映射、默认值设置等
- `@VerifyParams`: 多参数校验注解，可重复使用@VerifyParam注解

### 3. 返回结果类
- `R`: 统一返回结果封装类，提供了标准的 API 返回格式，支持cookie、header、重定向等扩展功能

### 4. 枚举类
- `ReturnCode`: 返回码枚举，定义了系统中使用的标准返回码，包括成功、失败、参数错误、权限不足等

### 5. 异常类
- `XcServiceException`: 自定义业务异常类，用于处理业务逻辑异常
- `XcRunException`: 运行时异常基类
- `XcToolsException`: 工具类异常
- `ParamErrorException`: 参数错误异常
- `InjectParamException`: 参数注入异常

### 6. 工具类
- `NetUtils`: 网络请求工具类，支持GET/POST请求、文件上传、多种Content-Type
- `BeanUtils`: 实体转换工具类，支持bean到bean、bean到map的转换
- `EncryptUtils`: 加密工具类，支持MD5和SHA1加密
- `Dict`: 常量字典类
- `Conversion`: 类型转换工具类
- `ImageTools`: 图片处理工具类
- `PdfTools`: PDF处理工具类

### 7. 拦截器和处理器
- `CoreInterceptor`: 核心拦截器，主要处理请求参数解析
- `XcGlobalExceptionHandler`: 全局异常处理器，统一处理各种异常并返回标准格式
- `VerifyParamHandlerMethodArgumentResolver`: 参数校验解析器
- `InjectEntityHandlerMethodArgumentResolver`: 实体注入解析器
- `EventHandlerMethodArgumentResolver`: 事件解析器

### 8. 参数校验责任链
- `AbstractVerifyChain`: 参数校验责任链抽象基类
- `CheckAnnotationChain`: 带参数的ParamCheck校验链
- `DefaultValueChain`: 默认值处理链
- `ParamCheckChain`: ParamCheck实现类校验链
- `ParamMapChain`: 参数预处理链
- `RegexCheckChain`: 正则校验链

### 9. 事件处理
- `Event`: 事件接口
- `EventInner`: 事件内部接口
- `EventImpl`: 事件实现类，基于ThreadLocal实现请求上下文数据存储

### 10. 转换器
- `String2EntityConverterFactory`: 字符串到实体转换器
- `String2ListGenericConverter`: 字符串到列表转换器
- `JsonObject2EntityConverterFactory`: JSON对象到实体转换器
- `JsonArray2ListGenericConverter`: JSON数组到列表转换器

### 11. 配置类
- `XcCorsConfig`: 跨域配置类，自动配置CORS跨域支持
- `FilterRegisterConfig`: 过滤器注册配置类

## 核心功能

### 1. 统一返回格式
- 提供了 `R` 类来封装 API 返回结果，确保返回格式的一致性
- 定义了标准的返回码 `ReturnCode` 枚举
- 支持cookie、header、重定向等扩展功能
- 支持自定义状态码和HTTP状态

### 2. 参数校验
- 提供了 `@VerifyParam` 和 `@VerifyParams` 注解用于参数校验
- 支持正则表达式校验
- 支持自定义校验类（实现ParamCheck接口）
- 支持参数映射和默认值设置
- 采用责任链模式实现参数校验流程，包括正则校验、自定义校验、默认值处理等

### 3. 异常处理
- 提供了多种自定义异常类用于处理不同类型的异常
- 全局异常处理器统一处理各种异常并返回标准格式
- 支持业务异常、参数异常、运行时异常等不同类型异常处理
- 自动记录异常日志

### 4. 实体注入
- 提供了 `@InjectEntity` 注解用于实体注入
- 支持从请求参数自动注入实体对象
- 支持JSON格式请求体自动解析为实体对象

### 5. 网络请求
- `NetUtils` 提供了完整的HTTP请求功能
- 支持GET/POST请求
- 支持文件上传
- 支持多种Content-Type（JSON、XML、表单、multipart）
- 支持Basic认证、自定义Header等

### 6. 实体转换
- `BeanUtils` 提供了强大的实体转换功能
- 支持bean到bean、bean到map的转换
- 支持字段忽略、自定义转换规则
- 支持基于Spring的类型转换机制

### 7. 加密工具
- `EncryptUtils` 提供了MD5和SHA1加密功能
- 支持字符串加密处理

### 8. 跨域支持
- 自动配置CORS跨域支持
- 支持自定义跨域配置
- 支持Origin、Header、Method白名单配置

### 9. 请求上下文管理
- 基于ThreadLocal实现请求上下文数据存储
- 支持请求参数、Token等数据的线程内共享
- 自动清理请求完成后的上下文数据

### 10. 类型转换
- 基于Spring的ConversionService实现类型转换
- 支持字符串到实体、列表等多种类型转换
- 支持自定义转换器注册

## 使用方式

核心启动模块是整个项目的基础设施，其他模块都依赖于此模块。在使用其他功能模块时，会自动引入核心启动模块。

在Spring Boot应用中，只需引入该模块依赖，即可自动启用核心功能：
1. 统一异常处理
2. 参数校验
3. 跨域支持
4. 请求参数解析
5. 返回结果标准化

## 配置参数

### 核心配置 (xc.core)
- `path`: 核心拦截路径，默认为`/**`
- `excludePath`: 核心拦截器排除路径，默认为`/error`
- `enable`: 是否启用核心功能，默认为`true`

### 认证配置 (xc.authentication)
- 支持多客户端配置
- `path`: 需要拦截的请求路径
- `excludePath`: 拦截器排除路径
- `tokenName`: Token Header名
- `expires`: Token有效期
- `userCacheExpires`: 用户缓存时间
- `allowedOnline`: 同时允许在线用户数
- `userCache`: 是否使用redis缓存用户信息

### 跨域配置 (xc.cors)
- `allowedOrigins`: Origin白名单
- `allowedHeaders`: Header白名单
- `allowedMethods`: Method白名单
- `path`: 处理路径

### Drone配置 (xc.drone)
- `host`: Api 接口地址
- `token`: Api Token
- `gitUser`: Git 用户名
- `gitPassword`: Git 密码

### 网关配置 (xc.gateway)
- `whiteIps`: 白名单，支持细化配置
- `blackIps`: 黑名单，仅支持全局生效

## 依赖模块

- Spring Boot相关依赖
- FastJSON2
- Hutool
- Guava
- Swagger
- Spring Web MVC
- Spring Context
- Spring Core