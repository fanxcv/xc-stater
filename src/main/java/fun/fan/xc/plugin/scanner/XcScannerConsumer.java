package fun.fan.xc.plugin.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 包扫描回调接口
 *
 * @author fan
 */
public interface XcScannerConsumer {
    /**
     * 包扫描回调接口
     *
     * @param clazz      Class定义
     * @param field      属性
     * @param annotation 注解
     */
    void accept(Class<?> clazz, Field field, Annotation annotation);
}
