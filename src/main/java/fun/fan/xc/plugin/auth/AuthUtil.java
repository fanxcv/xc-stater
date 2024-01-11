package fun.fan.xc.plugin.auth;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import fun.fan.xc.plugin.redis.Redis;
import fun.fan.xc.starter.exception.XcToolsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;
import java.util.Optional;

/**
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class AuthUtil {
    private final Redis redis;
    private final AuthConfigure authConfigure;

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
     */
    public void refreshUserInfo() {
        XcBaseUser user = AuthLocal.getUser();
        refreshUserInfo(user);
    }

    /**
     * 刷新用户信息
     * 支持传入指定用户
     */
    public void refreshUserInfo(XcBaseUser user) {
        if (Objects.nonNull(user)) {
            redis.del(String.format(AuthConstant.USER_PREFIX, user.getClient(), user.getAccount()));
        }
    }

    public String currentToken() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        return (String) attributes.getAttribute(AuthConstant.TOKEN, 0);
    }
}
