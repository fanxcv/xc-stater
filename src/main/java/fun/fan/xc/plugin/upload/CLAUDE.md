# MinIO文件上传模块 (upload)

## 模块职责

MinIO文件上传模块提供了基于MinIO对象存储的文件上传功能。该模块封装了MinIO客户端的操作，简化了文件上传、下载、删除等操作。

## 核心组件

### 1. 启动注解
- `@EnableUpload`: 启用文件上传功能的注解

### 2. 配置类
- `UploadConfig`: 文件上传配置类，管理MinIO相关配置

### 3. 核心类
- `MinioUtils`: MinIO工具类，提供文件上传、下载、删除等操作

## 核心功能

### 1. MinIO对象存储集成
- 支持文件上传、下载、删除操作
- 支持Bucket创建、删除、列表查询
- 支持文件和文件夹存在性检查
- 支持文件拷贝操作
- 支持批量文件删除
- 支持获取文件状态信息

### 2. 多种上传方式
- 支持MultipartFile上传
- 支持InputStream上传
- 支持指定文件类型上传

### 3. 文件访问管理
- 提供文件永久访问地址生成
- 支持文件前缀查询

## 使用方式

1. 在Spring Boot启动类上添加`@EnableUpload`注解
2. 在`application.yml`中配置MinIO相关参数
3. 通过依赖注入使用`MinioUtils`类进行文件操作

## 依赖模块

- MinIO Client