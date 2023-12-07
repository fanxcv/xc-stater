package fun.fan.xc.plugin.fastjson;

import java.lang.annotation.*;


/**
 * 字段转换
 *
 * @author fan
 */
@Inherited
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldConvert {
    /**
     * 原始数据字段名
     */
    String source() default "";

    /**
     * 转换类
     */
    Class<? extends IFieldConvert> covert();
}
