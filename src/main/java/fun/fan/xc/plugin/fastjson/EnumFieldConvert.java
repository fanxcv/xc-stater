package fun.fan.xc.plugin.fastjson;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * 枚举值转枚举名
 *
 * @author fan
 */
@Component
public class EnumFieldConvert implements IFieldConvert {
    @Override
    public Object covert(Object source, String name, AnnotationAttributes attributes) throws Exception {
        Class<?> type = attributes.getClass("type");
        String fieldName = attributes.getString("field");
        Field field = ReflectionUtils.findField(type, fieldName);
        Assert.notNull(field, "该枚举不存在" + fieldName + "属性, 枚举类型:" + type);
        field.setAccessible(true);
        return field.get(source);
    }
}
