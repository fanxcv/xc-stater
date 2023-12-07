package fun.fan.xc.plugin.fastjson;

import fun.fan.xc.plugin.scanner.XcScannerConsumer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * 用户账号转name注解扫描
 *
 * @author fan
 */
public class FieldConvertScannerConsumer implements XcScannerConsumer {
    @Override
    public void accept(Class<?> clazz, Field field, Annotation annotation) {
        String key = clazz.getName() + ":" + field.getName();
        AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(field, FieldConvert.class);
        if (Objects.nonNull(attributes)) {
            attributes.putAll(AnnotationUtils.getAnnotationAttributes(annotation));
        } else {
            attributes = AnnotationUtils.getAnnotationAttributes(annotation, false, false);
        }
        FieldCovertSerializer.CACHE.put(key, attributes);
    }
}
