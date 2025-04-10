package fun.fan.xc.plugin.meituan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({
        MtTokenService.class,
        MtRedisTokenManager.class,
        MtLocalTokenManager.class,
})
@EnableConfigurationProperties(MtConfig.class)
public @interface EnableMeiTuanApi {
}
