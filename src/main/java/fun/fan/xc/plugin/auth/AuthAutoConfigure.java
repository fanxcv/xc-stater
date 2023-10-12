package fun.fan.xc.plugin.auth;

import cn.hutool.core.lang.Assert;
import com.google.common.collect.Lists;
import fun.fan.xc.plugin.auth.interceptor.BaseAuthInterceptor;
import fun.fan.xc.plugin.auth.interceptor.XcAuthInterceptor;
import fun.fan.xc.plugin.auth.resolver.UserHandlerMethodArgumentResolver;
import fun.fan.xc.plugin.auth.service.XcAuthUserService;
import fun.fan.xc.plugin.redis.Redis;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户认证拦截器
 *
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class AuthAutoConfigure implements WebMvcConfigurer, ApplicationContextAware {
    private final XcAuthUserService userService;
    private final AuthConfigure authConfigure;
    private final AuthUtil authUtil;
    private final Redis redis;

    private Map<String, XcAuthInterceptor> beans;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        beans.forEach((k, v) -> {
            AuthConfigure.Configure configure = v.getConfigure(authConfigure);
            Set<String> excludePath = configure.getExcludePath();
            excludePath.addAll(AuthConstant.BASE_EXCLUDE_PATH);
            log.info("===> auth: init xc-boot-auth in {}, exclude {}", configure.getPath(), configure.getExcludePath());
            registry.addInterceptor(new BaseAuthInterceptor(redis, authUtil, authConfigure, userService, v))
                    .excludePathPatterns(Lists.newLinkedList(excludePath))
                    .addPathPatterns(configure.getPath())
                    .order(Ordered.HIGHEST_PRECEDENCE);
        });
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UserHandlerMethodArgumentResolver());
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.beans = applicationContext.getBeansOfType(XcAuthInterceptor.class);
        Assert.notEmpty(beans, "请先实现XcAuthInterceptor接口");
    }
}
