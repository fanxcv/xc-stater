package fun.fan.xc.plugin.redis;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        RedisConfigure.class,
        SpringRedisImpl.class
})
public @interface EnableRedis {
}
