package fun.fan.xc.plugin.auth.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import fun.fan.xc.plugin.auth.*;
import fun.fan.xc.plugin.auth.annotation.AuthIgnore;
import fun.fan.xc.plugin.auth.annotation.AuthPermission;
import fun.fan.xc.plugin.redis.Redis;
import fun.fan.xc.starter.enums.ReturnCode;
import fun.fan.xc.starter.exception.XcServiceException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 管理端认证拦截器
 *
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class BaseAuthInterceptor implements HandlerInterceptor {
    private final Redis redis;
    private final AuthUtil authUtil;
    private final AuthConfigure authConfigure;
    private final XcAuthInterface xcAuthInterface;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        // 判断接口是否需要做登录校验
        AuthIgnore authIgnore = hm.getMethodAnnotation(AuthIgnore.class);
        AuthConfigure.Configure configure = xcAuthInterface.getConfigure(authConfigure);
        String client = xcAuthInterface.client();
        // Token校验
        String token = xcAuthInterface.getToken(configure, request);
        if (StrUtil.isBlank(token)) {
            return checkIgnore(authIgnore);
        }
        // 全局缓存Token
        request.setAttribute(AuthConstant.TOKEN, token);
        String account = xcAuthInterface.getAccount(token);
        if (StrUtil.isBlank(account)) {
            return checkIgnore(authIgnore);
        }
        try {
            // 获取用户信息
            String key = String.format(AuthConstant.USER_PREFIX, client, account);
            XcBaseUser user;
            if (configure.isUserCache()) {
                user = redis.getOrLoadEx(key, configure.getUserCacheExpires().getSeconds(), TimeUnit.SECONDS, () -> xcAuthInterface.select(account));
            } else {
                user = xcAuthInterface.select(account);
            }
            Assert.isTrue(xcAuthInterface.checkUser(user), "用户异常");
            AuthLocal.setUser(user);
            authUtil.updateToken(token, client);

            // 权限校验
            AuthPermission annotation = hm.getMethodAnnotation(AuthPermission.class);
            if (Objects.isNull(annotation)) {
                return true;
            }
            String[] users = annotation.user();
            if (ArrayUtil.isNotEmpty(users) && !checkUsers(user, users)) {
                throw new XcServiceException(ReturnCode.FORBIDDEN);
            }
            String[] role = annotation.role();
            if (ArrayUtil.isNotEmpty(role) && !checkRoles(user, role)) {
                throw new XcServiceException(ReturnCode.FORBIDDEN);
            }
            String[] permission = annotation.permission();
            return checkPermissions(user, permission);
        } catch (IllegalArgumentException e) {
            log.error("read user info failed", e);
            return checkIgnore(authIgnore);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        AuthLocal.clearUser();
    }

    private boolean checkIgnore(AuthIgnore authIgnore) {
        if (Objects.nonNull(authIgnore)) {
            return true;
        } else {
            throw new XcServiceException(ReturnCode.UNAUTHORIZED);
        }
    }

    public boolean checkUsers(XcBaseUser user, String... users) {
        if (Objects.isNull(user) || StrUtil.isBlank(user.getAccount())) {
            return false;
        }
        if (Objects.isNull(users) || users.length == 0) {
            return false;
        }
        return Arrays.asList(users).contains(user.getAccount());
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
     * 校验权限, 如果没有设置权限, 就直接放过
     *
     * @param user       待校验的用户
     * @param permission 需要判断的权限列表
     * @return 校验结果
     */
    public boolean checkPermissions(XcBaseUser user, String... permission) {
        if (Objects.isNull(permission) || permission.length == 0) {
            return true;
        }
        String client = Optional.ofNullable(user.getClient()).orElse(AuthConstant.DEFAULT_CLIENT);
        // 查询用户权限
        String key = String.format(AuthConstant.PERMISSION_PREFIX, user.getClient(), user.getAccount());
        if (!redis.exists(key)) {
            AuthConfigure.Configure configure = authConfigure.getConfigureByClient(client);
            Set<String> permissions = Optional.ofNullable(xcAuthInterface.selectPermissions(user)).orElse(new HashSet<>());
            redis.sAddEx(key, configure.getExpires().getSeconds(), TimeUnit.SECONDS, permissions.toArray());
        }
        Object[] ps = Arrays.stream(permission).toArray();
        if (redis.sAnyIsMember(key, ps)) {
            return true;
        } else {
            throw new XcServiceException(ReturnCode.FORBIDDEN);
        }
    }
}
