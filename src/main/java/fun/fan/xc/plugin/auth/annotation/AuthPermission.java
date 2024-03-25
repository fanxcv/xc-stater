package fun.fan.xc.plugin.auth.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 权限校验, 用户, 角色, 权限同时设置时, 必须同时满足
 *
 * @author fan
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthPermission {
    @AliasFor("permission")
    String[] value() default {};

    /**
     * 有权限的用户列表
     */
    String[] user() default {};

    /**
     * 有权限的角色
     */
    String[] role() default {};

    /**
     * 有权限的权限
     */
    @AliasFor("value")
    String[] permission() default {};
}
