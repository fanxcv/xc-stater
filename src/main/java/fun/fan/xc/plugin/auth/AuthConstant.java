package fun.fan.xc.plugin.auth;

import com.google.common.collect.Lists;
import fun.fan.xc.plugin.redis.Redis;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

/**
 * 用户认证字典
 *
 * @author fan
 */
public class AuthConstant implements ApplicationContextAware {
    /**
     * 用户缓存前缀
     */
    public static final String USER_PREFIX = "auth:user:%s:%s";
    public static final String TOKEN_PREFIX = "auth:token:%s:%s";
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
    public static final String TOKEN = "XC_AUTH_TOKEN_ATTRIBUTE";
    static Redis redis;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AuthConstant.redis = applicationContext.getBean(Redis.class);
    }
}
