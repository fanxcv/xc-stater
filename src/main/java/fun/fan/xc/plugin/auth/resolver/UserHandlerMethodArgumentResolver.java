package fun.fan.xc.plugin.auth.resolver;

import fun.fan.xc.plugin.auth.AuthLocal;
import fun.fan.xc.plugin.auth.XcBaseUser;
import fun.fan.xc.plugin.auth.annotation.AuthUser;
import fun.fan.xc.starter.enums.ReturnCode;
import fun.fan.xc.starter.exception.XcRunException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 用户信息注入注解实现
 *
 * @author fan
 */
@Slf4j
public class UserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        try {
            MethodParameter nestedIfOptional = parameter.nestedIfOptional();
            AuthUser authUser = nestedIfOptional.getParameterAnnotation(AuthUser.class);
            Assert.notNull(authUser, "AuthUser is not be null");
            XcBaseUser user = AuthLocal.getUser();
            if (authUser.required()) {
                Assert.notNull(user, "user info is required");
            }
            return user;
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new XcRunException(ReturnCode.UNAUTHORIZED);
        }
    }
}
