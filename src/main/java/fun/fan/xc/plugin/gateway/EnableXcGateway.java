package fun.fan.xc.plugin.gateway;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ImportAutoConfiguration(DefaultGatewayHandler.class)
public @interface EnableXcGateway {
}
