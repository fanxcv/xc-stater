# 核心启动模块 (fun.fan.xc.starter)

## 模块职责

该模块是整个框架的核心启动模块，提供了基础配置、异常处理、参数校验、数据转换等核心功能。

## 核心组件

### 自动配置类
- `XcCoreAutoConfiguration`: 核心自动配置类，负责注册各种核心组件

### 异常处理类
- `XcGlobalExceptionHandler`: 全局异常处理器
- `XcGlobalDataExceptionHandler`: 全局数据异常处理器

### 参数处理类
- `VerifyParamHandlerMethodArgumentResolver`: 参数校验处理器
- `InjectEntityHandlerMethodArgumentResolver`: 实体注入处理器
- `EventHandlerMethodArgumentResolver`: 事件处理器参数解析器

### 责任链类
- `AbstractVerifyChain`: 参数校验责任链基类
- `ParamMapChain`: 参数映射链
- `RegexCheckChain`: 正则校验链
- `CheckAnnotationChain`: 注解校验链
- `DefaultValueChain`: 默认值链
- `ParamCheckChain`: 参数检查链

### 配置类
- `XcCorsConfig`: CORS配置类
- `FilterRegisterConfig`: 过滤器注册配置类

### 工具类
- `BaseVerify`: 基础校验类
- `XcEnvironmentPostProcessor`: 环境后置处理器

### 枚举类
- `ReturnCode`: 返回码枚举

### 转换器类
- `String2ListGenericConverter`: 字符串到列表转换器
- `JsonArray2ListGenericConverter`: JSON数组到列表转换器
- `JsonObject2EntityConverterFactory`: JSON对象到实体转换器工厂
- `String2EntityConverterFactory`: 字符串到实体转换器工厂

## 关键文件

### 自动配置类
- `XcCoreAutoConfiguration.kt`: 核心自动配置类，负责：
  - 注册全局异常处理器
  - 注册参数处理解析器
  - 配置CORS支持
  - 注册过滤器
  - 配置数据转换器

### 异常处理类
- `XcGlobalExceptionHandler.kt`: 全局异常处理器，处理系统异常
- `XcGlobalDataExceptionHandler.kt`: 全局数据异常处理器，处理数据相关异常

### 参数处理类
- `VerifyParamHandlerMethodArgumentResolver.kt`: 参数校验处理器，负责：
  - 解析请求参数
  - 执行参数校验
  - 处理校验结果

- `InjectEntityHandlerMethodArgumentResolver.kt`: 实体注入处理器，负责：
  - 将请求数据注入到实体对象
  - 处理实体校验

- `EventHandlerMethodArgumentResolver.kt`: 事件处理器参数解析器，负责：
  - 解析事件处理器参数

### 责任链类
- `AbstractVerifyChain.kt`: 参数校验责任链基类，定义责任链基础结构
- `ParamMapChain.kt`: 参数映射链，处理参数映射
- `RegexCheckChain.kt`: 正则校验链，执行正则表达式校验
- `CheckAnnotationChain.kt`: 注解校验链，处理注解校验
- `DefaultValueChain.kt`: 默认值链，设置参数默认值
- `ParamCheckChain.kt`: 参数检查链，执行参数检查

### 配置类
- `XcCorsConfig.kt`: CORS配置类，配置跨域支持
- `FilterRegisterConfig.kt`: 过滤器注册配置类，注册请求过滤器

### 工具类
- `BaseVerify.kt`: 基础校验类，提供通用校验方法
- `XcEnvironmentPostProcessor.kt`: 环境后置处理器，处理环境配置

### 枚举类
- `ReturnCode.kt`: 返回码枚举，定义系统返回码

### 转换器类
- `String2ListGenericConverter.kt`: 字符串到列表转换器
- `JsonArray2ListGenericConverter.kt`: JSON数组到列表转换器
- `JsonObject2EntityConverterFactory.kt`: JSON对象到实体转换器工厂
- `String2EntityConverterFactory.kt`: 字符串到实体转换器工厂

## 接口规范

### 自动配置规范
1. 实现Spring Boot的自动配置机制
2. 提供条件化的Bean注册
3. 支持外部配置属性
4. 实现灵活的组件装配

### 异常处理规范
1. 实现全局异常捕获
2. 提供统一的错误响应格式
3. 支持自定义异常处理
4. 实现详细的错误日志记录

### 参数处理规范
1. 实现请求参数自动解析
2. 提供灵活的参数校验机制
3. 支持注解方式配置校验规则
4. 实现责任链模式处理参数

## 依赖关系

### 外部依赖
- `Spring Boot`: Spring框架
- `FastJSON2`: JSON处理
- `Hutool`: 工具类库
- `Guava`: Google工具库

### 内部依赖
- `fun.fan.xc.starter.utils`: 工具类模块
- `fun.fan.xc.starter.exception`: 异常处理模块

## 测试要点

1. 自动配置功能的正确性测试
2. 异常处理机制的完整性测试
3. 参数校验功能的准确性测试
4. 数据转换器的正确性测试
5. 责任链模式的链式调用测试

## 编码规范

1. 使用Kotlin编写核心逻辑
2. 遵循Spring Boot的配置规范
3. 提供完整的注释说明
4. 实现统一的异常处理
5. 使用责任链模式优化处理流程