# 大陆CA电子合同模块 (ca)

## 模块职责

大陆CA电子合同模块是针对"大陆CA电子合同"签章系统的调用封装，提供了与该系统交互的客户端功能。该模块封装了与大陆CA电子合同系统API的调用细节，简化了在项目中集成电子合同签署功能的复杂性。

## 核心组件

### 1. 启动注解
- `@EnableMcsClient`: 启用大陆CA电子合同客户端功能的注解，位于mcs子包中

### 2. 核心类
- `McsClient`: 大陆CA电子合同客户端，负责与大陆CA电子合同系统进行API交互
- `McsConfig`: 大陆CA电子合同配置类，管理与大陆CA电子合同系统交互所需的配置参数
- `McsUtils`: 大陆CA电子合同工具类，提供签名等辅助功能

### 3. 实体类
- `Request`: 请求对象封装类
- `Response`: 响应对象封装类
- `ReqHead`: 请求头对象
- `ResHead`: 响应头对象
- `McsBody`: 请求体基类

### 4. 注解类
- `@McsPath`: 用于标识请求路径的注解

## 核心功能

### 1. 电子合同系统集成
- 提供了与大陆CA电子合同系统API的调用封装
- 支持请求签名和验签功能
- 支持文件下载功能

### 2. 配置管理
- 提供了与大陆CA电子合同系统交互所需的配置项管理
- 支持调试模式配置

## 使用方式

1. 在 Spring Boot 启动类上添加 `@EnableMcsClient` 注解
2. 在 `application.yml` 或 `application.properties` 中配置大陆CA电子合同相关参数：
   - `xc.ca.mcs.apiHost`: 接口地址
   - `xc.ca.mcs.appId`: 签约机构应用ID
   - `xc.ca.mcs.customerId`: 签约机构编码
   - `xc.ca.mcs.privateKey`: 机构私钥
   - `xc.ca.mcs.publicKey`: 机构公钥

## 依赖模块

- 需要引入大陆CA提供的pki-core依赖包