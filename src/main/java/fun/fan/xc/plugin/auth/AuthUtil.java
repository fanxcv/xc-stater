package fun.fan.xc.plugin.auth;

import cn.hutool.core.lang.UUID;
import fun.fan.xc.plugin.redis.Redis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
        String client = Optional.ofNullable(user.getClient()).orElse("default");
        AuthConfigure.Configure configure = authConfigure.getConfigureByClient(client);
        redis.setEx(AuthConstant.TOKEN_PREFIX + token, user.getAccount(), configure.getExpires(), TimeUnit.MINUTES);
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
        redis.expire(AuthConstant.TOKEN_PREFIX + token, configure.getExpires(), TimeUnit.MINUTES);
    }

    /**
     * 移除Token
     *
     * @param token 待移除Token
     */
    public boolean removeToken(String token) {
        String key = AuthConstant.TOKEN_PREFIX + token;
        // 移除缓存的用户信息
        redis.del(AuthConstant.USER_PREFIX + redis.get(key));
        return redis.del(key) > 0L;
    }
}
