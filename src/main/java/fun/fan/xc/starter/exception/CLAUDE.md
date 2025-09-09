# 异常处理模块 (fun.fan.xc.starter.exception)

## 模块职责

该模块定义了项目中使用的自定义异常类，用于统一异常处理和错误信息管理。

## 核心组件

### 基础异常类
- `XcServiceException`: 服务异常类，用于处理业务逻辑异常
- `XcRunException`: 运行时异常类，用于处理运行时错误
- `XcToolsException`: 工具异常类，用于处理工具类相关异常

### 参数异常类
- `ParamErrorException`: 参数错误异常类，用于处理参数校验失败
- `InjectParamException`: 参数注入异常类，用于处理参数注入失败

## 关键文件

### 基础异常类
- `XcServiceException.kt`: 服务异常类，继承自RuntimeException，用于：
  - 处理业务逻辑异常
  - 提供错误码和错误信息
  - 支持链式异常处理

- `XcRunException.kt`: 运行时异常类，继承自RuntimeException，用于：
  - 处理系统运行时错误
  - 提供详细的错误信息
  - 支持自定义错误码

- `XcToolsException.kt`: 工具异常类，继承自RuntimeException，用于：
  - 处理工具类相关异常
  - 提供工具操作错误信息
  - 支持异常链追踪

### 参数异常类
- `ParamErrorException.kt`: 参数错误异常类，继承自XcServiceException，用于：
  - 处理参数校验失败
  - 提供参数错误详细信息
  - 支持字段级错误定位

- `InjectParamException.kt`: 参数注入异常类，继承自XcServiceException，用于：
  - 处理参数注入失败
  - 提供注入错误信息
  - 支持注入点定位

## 接口规范

### 异常处理规范
1. 继承合适的父异常类
2. 提供清晰的错误信息
3. 支持错误码机制
4. 实现链式异常追踪
5. 保持异常处理的一致性

## 依赖关系

### 外部依赖
- `Spring Boot`: Spring框架

### 内部依赖
- `fun.fan.xc.starter`: 核心启动模块

## 测试要点

1. 异常构造函数的正确性测试
2. 错误信息和错误码的准确性测试
3. 异常链追踪的完整性测试
4. 异常处理的性能测试

## 编码规范

1. 使用Kotlin编写异常类
2. 提供完整的构造函数重载
3. 实现标准的异常处理模式
4. 保持异常类的简洁性
5. 遵循Java异常处理最佳实践