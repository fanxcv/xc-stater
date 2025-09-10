# 认证授权模块使用说明

## 1. 启用认证模块

在Spring Boot启动类上添加`@EnableAuth`注解：

```java
@SpringBootApplication
@EnableAuth
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 2. 配置文件添加认证相关配置

```yaml
xc:
  authentication:
    default: # 配置名，需要与XcAuthInterface实现的client()方法返回值一致
      token-name: M-Token # 请求中携带Token的Header名称
      expires: 2h # Token过期时间
      user-cache: true # 是否缓存用户信息
      user-cache-expires: 30m # 用户信息缓存过期时间
      path: # 需要拦截的路径
        - /**
```

如果需要配置多个客户端，可以添加更多配置项：

```yaml
xc:
  authentication:
    admin: # 管理端配置
      token-name: Admin-Token
      expires: 2h
      user-cache: true
      user-cache-expires: 30m
      path:
        - /admin/**
    user: # 用户端配置
      token-name: User-Token
      expires: 7d
      user-cache: true
      user-cache-expires: 1h
      path:
        - /api/**
```

## 3. 创建用户实体

该实体需要实现`XcBaseUser`接口：

```java
@Data
@Accessors(chain = true)
public class UserEntity implements XcBaseUser {
    private String id;
    private String name;


    /**
     * 该字段并不需要存入数据库
     */
    @TableField(exist = false)
    @Schema(description = "登录返回Token")
    private String token;

    /**
     * 该字段并不需要存入数据库, 也不需要返回到前端
     */
    @Hidden
    @JsonIgnore
    @TableField(exist = false)
    @JSONField(serialize = false)
    private String client = "default";

    @Override
    public String getAccount() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getClient() {
        return this.client;
    }

    @Override
    public void setClient(String client) {
        this.client = client;
    }
}
```

## 4. 实现`XcAuthInterface`接口

```java
@Component
public class XcAuth implements XcAuthInterface {
    /**
     * 返回当前项目的配置名称, 需要与配置文件中的配置名一致
     */
    @Override
    public String client() {
        return "default";
    }

    /**
     * 校验用户是否有效
     *
     * @param user 待校验的用户对象
     * @return 校验结果
     */
    @Override
    public boolean checkUser(XcBaseUser user) {
        // 可以在这里添加用户状态校验逻辑，如是否被禁用等
        return user != null;
    }

    /**
     * 获取用户权限列表
     *
     * @param user 当前用户
     * @return 权限列表
     */
    @Override
    public Set<String> selectPermissions(XcBaseUser user) {
        // 在这里实现获取用户权限列表的逻辑
        // 如果不需要权限校验，可以返回null或空集合
        return null;
    }

    /**
     * 该方法主要用于通过account查询用户信息, 返回的用户信息会被缓存到redis, 用于@AuthUser注入
     *
     * @param account {@link XcBaseUser#getAccount()}返回的数据
     * @return 查询到的用户对象
     */
    @Override
    public XcBaseUser select(String account) {
        // TODO 执行用户查询
        // 根据account查询用户信息并返回UserEntity对象
        return null;
    }
}
```

## 5. 使用认证功能

### 5.1. 认证拦截机制

接口默认都会被认证拦截器拦截，只有在配置中排除的路径或者添加了`@AuthIgnore`注解的接口才不需要认证。

对于需要认证的接口，系统会自动从请求头中获取Token并进行校验。如果Token无效或过期，将返回401未授权错误。

### 5.2. 用户信息注入

`@AuthUser`注解用于参数注入已登录用户信息，与接口是否需要认证无关。只有在方法参数中使用了`@AuthUser`注解时，系统才会尝试注入当前登录的用户信息。

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @GetMapping("/user/info")
    public R<UserEntity> getUserInfo(@AuthUser XcBaseUser user) {
        // @AuthUser注解用于参数注入已登录用户信息
        return R.success((UserEntity) user);
    }
    
    @GetMapping("/public/info")
    @AuthIgnore
    public R<String> getPublicInfo() {
        // 此接口不需要认证，即使方法中没有@AuthUser注解
        return R.success("公共信息");
    }
    
    @GetMapping("/need/auth")
    public R<String> needAuth() {
        // 此接口需要认证，但方法中没有@AuthUser注解
        // 可以通过AuthLocal.getUser()获取用户信息
        XcBaseUser user = AuthLocal.getUser();
        return R.success("需要认证的接口");
    }
}
```

`@AuthUser`注解有一个`required()`属性，默认值为true，表示用户信息是必须的。如果设置为false，则在用户未登录时参数值为null。

### 5.2. 权限校验

使用`@AuthPermission`注解进行权限校验：

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @GetMapping("/admin/users")
    @AuthPermission(permission = "user:manage")
    public R<List<UserEntity>> getUsers(@AuthUser XcBaseUser user) {
        // 只有拥有"user:manage"权限的用户才能访问此接口
        // @AuthUser用于参数注入用户信息
        return R.success(userService.list());
    }
    
    @GetMapping("/admin/sensitive")
    @AuthPermission(role = "admin")
    public R<String> sensitiveOperation(@AuthUser XcBaseUser user) {
        // 只有角色为"admin"的用户才能访问此接口
        // @AuthUser用于参数注入用户信息
        return R.success("敏感操作执行成功");
    }
}
```

### 5.3. 忽略认证

对于不需要认证的接口，可以使用`@AuthIgnore`注解：

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @GetMapping("/public/info")
    @AuthIgnore
    public R<String> getPublicInfo() {
        return R.success("公共信息");
    }
}
```

## 6. AuthUtil工具类使用

### 6.1. 创建Token

```java
@Autowired
private AuthUtil authUtil;

public R<String> login(LoginRequest request) {
    // 验证用户身份
    UserEntity user = userService.validateUser(request.getUsername(), request.getPassword());
    if (user != null) {
        // 创建Token
        String token = authUtil.createToken(user);
        user.setToken(token);
        return R.success(user);
    }
    return R.fail("用户名或密码错误");
}
```

### 6.2. 刷新用户信息

```java
@PutMapping("/user/profile")
public R<UserEntity> updateUserProfile(@AuthUser XcBaseUser user, @RequestBody UserProfileRequest request) {
    // 更新用户信息
    userService.updateProfile(user.getAccount(), request);
    // 刷新用户信息缓存
    UserEntity updatedUser = authUtil.refreshUserInfo();
    return R.success(updatedUser);
}
```

### 6.3. 移除Token

```java
@DeleteMapping("/logout")
public R<Void> logout(@AuthUser XcBaseUser user) {
    String token = authUtil.currentToken();
    authUtil.removeToken(token);
    return R.success();
}
```

## 7. 获取当前用户信息

### 7.1. 通过AuthLocal获取

```java
@GetMapping("/user/info")
public R<UserEntity> getUserInfo() {
    UserEntity user = AuthLocal.getUser();
    return R.success(user);
}
```

### 7.2. 通过参数注入获取

```java
@GetMapping("/user/info")
public R<UserEntity> getUserInfo(@AuthUser XcBaseUser user) {
    // user参数会自动注入当前认证用户信息
    return R.success((UserEntity) user);
}
```

## 8. 注意事项

1. 需要启用Redis支持，因为Token和用户信息缓存依赖Redis
2. 配置文件中的client名称必须与XcAuthInterface实现的client()方法返回值一致
3. XcBaseUser实现类中的client字段需要正确设置，以匹配相应的配置
4. 权限校验是可选的，如果不需要可以不实现selectPermissions方法或返回null