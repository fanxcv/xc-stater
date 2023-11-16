package fun.fan.xc.plugin.auth.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 权限校验
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

    String[] role() default {};

    @AliasFor("value")
    String[] permission() default {};
}
