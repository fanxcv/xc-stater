1. 配置文件添加认证相关配置
   ```yaml
   xc:
     auth:
       default:
         token-name: M-Token # 请求中携带Token的Header名称
         path: # 需要拦截的路径
           - /**
   ```
2. 创建用户实体, 该实体需要实现`XcBaseUser`接口
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
3. 实现`XcAuthInterface`接口
   ```java
   @Component
   public class XcAuth implements XcAuthInterface {
       /**
        * 返回当前项目的配置名称, 需要与{@link XcBaseUser#getClient()}一致
        */
       @Override
       public String client() {
           return "default";
       }
   
       /**
        * 校验用户是否有效
        *
        * @param user 待校验的用户对象
        */
       @Override
       public boolean checkUser(XcBaseUser user) {
           return user != null;
       }
   
       /**
        * 该方法主要用于通过account查询用户信息, 返回的用户信息会被缓存到redis, 用于@AuthUser注入
        *
        * @param client  {@link XcBaseUser#getClient()}返回的client
        * @param account {@link XcBaseUser#getAccount()}返回的数据
        * @return 查询到的用户对象
        */
       @Override
       public XcBaseUser select(String client, String account) {
           // TODO 执行用户查询
           return null;
       }
   }
   ```
