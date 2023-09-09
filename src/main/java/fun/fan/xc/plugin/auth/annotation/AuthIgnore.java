package fun.fan.xc.plugin.auth.annotation;

import java.lang.annotation.*;

/**
 * 接口允许直接访问
 *
 * @author fan
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthIgnore {
}
