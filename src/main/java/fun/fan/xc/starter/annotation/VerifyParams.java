package fun.fan.xc.starter.annotation;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface VerifyParams {
    VerifyParam[] value();
}
