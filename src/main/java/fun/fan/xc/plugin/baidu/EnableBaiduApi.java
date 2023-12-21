package fun.fan.xc.plugin.baidu;

import fun.fan.xc.plugin.baidu.cloud.BaiduCloudTokenManager;
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
@Import({BaiduCloudTokenManager.class})
@EnableConfigurationProperties(BaiduConfig.class)
public @interface EnableBaiduApi {
}
