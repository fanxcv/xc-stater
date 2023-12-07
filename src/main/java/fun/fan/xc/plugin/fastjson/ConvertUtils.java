package fun.fan.xc.plugin.fastjson;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Objects;

public class ConvertUtils {
    public static <T> Field checkAndGetField(Object code, String name, AnnotationAttributes attributes, Class<T> clazz) {
        if (Objects.isNull(code)) {
            return null;
        }
        String a = String.valueOf(code);
        if (StrUtil.isBlank(a)) {
            return null;
        }
        String key = StrUtil.blankToDefault(attributes.getString("field"), name);
        return ReflectionUtils.findField(clazz, key);
    }
}
