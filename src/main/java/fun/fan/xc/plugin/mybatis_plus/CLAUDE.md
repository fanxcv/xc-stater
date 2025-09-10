# MyBatis Plus 扩展模块 (mybatis_plus)

## 模块职责

MyBatis Plus 扩展模块提供了 MyBatis Plus 的分页插件配置和枚举类型处理功能，简化了分页查询和枚举类型映射的实现。

## 核心组件

### 1. 配置类
- `XcMybatisPlusConfig`: MyBatis Plus 配置类，自动配置分页插件

### 2. 核心类
- `MybatisPlusInterceptor`: MyBatis Plus 拦截器
- `PaginationInnerInterceptor`: 分页内部拦截器
- `MybatisEnumTypeHandler`: 自定义MyBatis枚举类型处理器
- `EnumValue`: 枚举值标记注解

## 核心功能

### 1. 分页插件配置
- 自动配置 MyBatis Plus 的分页插件
- 默认配置 MySQL 数据库类型的分页拦截器
- 设置最大分页限制为 500 条记录

### 2. 枚举类型处理
- 提供自定义的MyBatis枚举类型处理器
- 支持枚举类型与数据库字段的映射
- 通过`@EnumValue`注解标记枚举的值字段

## 使用方式

该模块为自动配置模块，无需手动启用注解。当项目中引入 MyBatis Plus 依赖时，分页插件会自动生效。

对于枚举类型处理，可以在枚举类的字段上使用`@EnumValue`注解来标记该字段作为数据库存储的值。

## 依赖模块

- MyBatis Plus