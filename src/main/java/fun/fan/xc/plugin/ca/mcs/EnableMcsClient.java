package fun.fan.xc.plugin.ca.mcs;

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
@EnableConfigurationProperties(McsConfig.class)
@Import({McsUtils.class, McsClient.class})
public @interface EnableMcsClient {
}
