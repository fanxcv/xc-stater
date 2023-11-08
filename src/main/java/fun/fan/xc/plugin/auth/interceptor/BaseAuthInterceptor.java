package fun.fan.xc.plugin.auth.interceptor;

import cn.hutool.core.util.StrUtil;
import fun.fan.xc.plugin.auth.*;
import fun.fan.xc.plugin.auth.annotation.AuthIgnore;
import fun.fan.xc.plugin.auth.service.XcAuthUserService;
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
    private final XcAuthUserService authUserService;
    private final XcAuthInterceptor interceptor;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 判断接口是否需要做登录校验
        AuthIgnore authIgnore = ((HandlerMethod) handler).getMethodAnnotation(AuthIgnore.class);
        AuthConfigure.Configure configure = interceptor.getConfigure(authConfigure);
        String client = interceptor.client();
        // Token校验
        String token = interceptor.getToken(configure, request);
        if (StrUtil.isBlank(token)) {
            return checkIgnore(authIgnore);
        }
        String account = redis.get(AuthConstant.TOKEN_PREFIX + token);
        if (StrUtil.isBlank(account)) {
            return checkIgnore(authIgnore);
        }
        // 获取用户信息
        String key = String.format(AuthConstant.USER_PREFIX, client, account);
        try {
            XcBaseUser user = redis.getOrLoadEx(key, configure.getUserCacheExpires(), TimeUnit.MINUTES, () -> authUserService.select(client, account));
            Assert.isTrue(interceptor.checkUser(user), "用户异常");
            AuthLocal.setUser(user);
            authUtil.updateToken(token, client);
        } catch (IllegalArgumentException e) {
            log.error("read user info failed", e);
            return checkIgnore(authIgnore);
        }
        return true;
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
