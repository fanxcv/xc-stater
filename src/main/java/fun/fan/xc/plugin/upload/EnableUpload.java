package fun.fan.xc.plugin.upload;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        MinioUtils.class
})
@EnableConfigurationProperties(UploadConfig.class)
public @interface EnableUpload {
}
