package fun.fan.xc.plugin.auth;

import fun.fan.xc.plugin.redis.EnableRedis;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Documented
@EnableRedis
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        AuthUtil.class,
        AuthAutoConfigure.class
})
@ImportAutoConfiguration(AuthConfigure.class)
public @interface EnableAuth {
}
