package fun.fan.xc.plugin.auth;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import fun.fan.xc.plugin.redis.Redis;
import fun.fan.xc.starter.exception.XcToolsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class AuthUtil implements ApplicationContextAware {
    private final Redis redis;
    private final AuthConfigure authConfigure;
    private final XcAuthInterface xcAuthInterface;

    /**
     * 创建 Token
     *
     * @param user 用户信息
     * @return token
     */
    public String createToken(XcBaseUser user) {
        String token = UUID.fastUUID().toString(true);
        String client = Optional.ofNullable(user.getClient()).orElse(AuthConstant.DEFAULT_CLIENT);
        AuthConfigure.Configure configure = authConfigure.getConfigureByClient(client);
        redis.setEx(String.format(AuthConstant.TOKEN_PREFIX, client, token), user.getAccount(), configure.getExpires().getSeconds());
        user.setToken(token);
        // TODO 同时登陆限制
        return token;
    }

    /**
     * 续期Token
     *
     * @param token Token
     */
    public void updateToken(String token, String client) {
        AuthConfigure.Configure configure = authConfigure.getConfigureByClient(client);
        redis.expire(String.format(AuthConstant.TOKEN_PREFIX, client, token), configure.getExpires().getSeconds());
    }

    /**
     * 移除Token
     *
     * @param token 待移除Token
     */
    public boolean removeToken(String token) {
        XcBaseUser user = AuthLocal.getUser();
        Assert.notNull(user, "Token关联的用户信息异常");
        String key = String.format(AuthConstant.TOKEN_PREFIX, user.getClient(), token);
        if (user.getAccount().equals(redis.get(key))) {
            // 移除缓存的用户信息
            redis.del(String.format(AuthConstant.USER_PREFIX, user.getClient(), user.getAccount()));
            return redis.del(key) > 0L;
        } else {
            throw new XcToolsException("移出Token失败");
        }
    }

    /**
     * 刷新用户信息
     * @return 新的用户信息
     */
    public XcBaseUser refreshUserInfo() {
        XcBaseUser user = AuthLocal.getUser();
        return refreshUserInfo(user);
    }

    /**
     * 刷新用户信息
     * 支持传入指定用户
     * @return 新的用户信息
     */
    public XcBaseUser refreshUserInfo(XcBaseUser u) {
        if (Objects.isNull(u)) {
            return null;
        }

        String key = String.format(AuthConstant.USER_PREFIX, u.getClient(), u.getAccount());
        AuthConfigure.Configure configure = authConfigure.getConfigureByClient(u.getClient());
        XcBaseUser user = xcAuthInterface.select(u.getAccount());
        Assert.isTrue(xcAuthInterface.checkUser(user), "用户异常");

        if (configure.isUserCache()) {
            redis.setEx(key, user, configure.getUserCacheExpires().getSeconds(), TimeUnit.SECONDS);
        } else {
            redis.del(key);
        }

        AuthLocal.setUser(user);

        return user;
    }

    public String currentToken() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        return (String) attributes.getAttribute(AuthConstant.TOKEN, RequestAttributes.SCOPE_REQUEST);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AuthConstant.redis = applicationContext.getBean(Redis.class);
    }
}
