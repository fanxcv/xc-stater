package fun.fan.xc.plugin.auth;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 用户认证字典
 *
 * @author fan
 */
public class AuthConstant {
    /**
     * 用户缓存前缀
     */
    public static final String USER_PREFIX = "auth:user:%s:%s";
    public static final String TOKEN_PREFIX = "auth:token:";
    public static final String PERMISSION_PREFIX = "auth:permission:%s:%s";
    public static final String DEFAULT_CLIENT = "default";
    /**
     * 基础放行路径
     */
    public static final List<String> BASE_EXCLUDE_PATH = Lists.newArrayList("/error",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/doc.html");
}
