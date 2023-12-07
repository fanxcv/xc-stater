package fun.fan.xc.plugin.fastjson;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;


/**
 * 字段转换
 *
 * @author fan
 */
@Inherited
@Documented
@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@FieldConvert(covert = EnumFieldConvert.class)
public @interface EnumConvert {
    /**
     * 原始数据字段名
     */
    @AliasFor(
            annotation = FieldConvert.class,
            attribute = "source"
    )
    String source() default "";

    /**
     * 字段枚举类型
     */
    Class<? extends Enum<?>> type();

    /**
     * 枚举取值字段名
     */
    String field() default "desc";
}
