package fun.fan.xc.plugin.auth.interceptor;

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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
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
        String account = redis.get(AuthConstant.TOKEN_PREFIX + token);
        if (StrUtil.isBlank(account)) {
            return checkIgnore(authIgnore);
        }
        try {
            // 获取用户信息
            String key = String.format(AuthConstant.USER_PREFIX, client, account);
            XcBaseUser user = redis.getOrLoadEx(key, configure.getUserCacheExpires(), TimeUnit.MINUTES, () -> {
                XcBaseUser u = xcAuthInterface.select(client, account);
                Assert.isTrue(Objects.equals(client, u.getClient()), "查询的用户client与配置的client不一致");
                return u;
            });
            Assert.isTrue(xcAuthInterface.checkUser(user), "用户异常");
            AuthLocal.setUser(user);
            authUtil.updateToken(token, client);

            // 权限校验
            AuthPermission annotation = hm.getMethodAnnotation(AuthPermission.class);
            if (Objects.isNull(annotation)) {
                return true;
            }
            String[] role = annotation.role();
            if (ArrayUtil.isNotEmpty(role) && !authUtil.checkRoles(user, role)) {
                throw new XcServiceException(ReturnCode.FORBIDDEN);
            }
            String[] permission = annotation.permission();
            return authUtil.checkPermissions(user, permission);
        } catch (IllegalArgumentException e) {
            log.error("read user info failed", e);
            return checkIgnore(authIgnore);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) throws Exception {
        AuthLocal.clearUser();
    }

    private boolean checkIgnore(AuthIgnore authIgnore) {
        if (Objects.nonNull(authIgnore)) {
            return true;
        } else {
            throw new XcServiceException(ReturnCode.UNAUTHORIZED);
        }
    }
}
