package fun.fan.xc.plugin.auth.annotation;

import java.lang.annotation.*;

/**
 * 用户信息注入
 *
 * @author fan
 */
@Inherited
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
    /**
     * 是否必须
     */
    boolean required() default true;
}
