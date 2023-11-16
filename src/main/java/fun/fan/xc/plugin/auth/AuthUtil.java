package fun.fan.xc.plugin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import fun.fan.xc.plugin.redis.Redis;
import fun.fan.xc.starter.exception.XcToolsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class AuthUtil {
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
        XcBaseUser user = AuthLocal.getUser();
        String key = AuthConstant.TOKEN_PREFIX + token;
        if (Objects.nonNull(user) && user.getAccount().equals(redis.get(key))) {
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

    /**
     * 角色校验
     *
     * @param user  待校验的用户
     * @param roles 需要判断的角色列表
     * @return 校验结果
     */
    public boolean checkRoles(XcBaseUser user, String... roles) {
        if (Objects.isNull(user) || CollUtil.isEmpty(user.getRoles())) {
            return false;
        }
        if (Objects.isNull(roles) || roles.length == 0) {
            return false;
        }
        Collection<String> c = user.getRoles();
        return Arrays.stream(roles).anyMatch(c::contains);
    }

    /**
     * 校验权限
     *
     * @param user       待校验的用户
     * @param permission 需要判断的权限列表
     * @return 校验结果
     */
    public boolean checkPermissions(XcBaseUser user, String... permission) {
        if (Objects.isNull(permission) || permission.length == 0) {
            return false;
        }
        String client = Optional.ofNullable(user.getClient()).orElse(AuthConstant.DEFAULT_CLIENT);
        // 查询用户权限
        String key = String.format(AuthConstant.PERMISSION_PREFIX, user.getClient(), user.getAccount());
        if (!redis.exists(key)) {
            AuthConfigure.Configure configure = authConfigure.getConfigureByClient(client);
            Set<String> permissions = Optional.ofNullable(xcAuthInterface.selectPermissions(user)).orElse(new HashSet<>());
            redis.sAddEx(key, configure.getExpires(), TimeUnit.MINUTES, permissions.toArray());
        }
        return redis.sIsMember(key, permission);
    }
}
