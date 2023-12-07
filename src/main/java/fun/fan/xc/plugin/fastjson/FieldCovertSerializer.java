package fun.fan.xc.plugin.fastjson;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * fastjson 属性转换序列化器实现
 *
 * @author fan
 */
@Slf4j
@RequiredArgsConstructor
public class FieldCovertSerializer implements ValueFilter {
    public static final Map<String, AnnotationAttributes> CACHE = Maps.newConcurrentMap();
    private final BeanFactory beanFactory;

    @Override
    public Object apply(Object object, String name, Object value) {
        Class<?> clazz = object.getClass();
        String key = clazz.getName() + ":" + name;
        AnnotationAttributes attributes = CACHE.get(key);
        if (Objects.isNull(attributes)) {
            return value;
        }
        try {
            String source = attributes.getString("source");
            if (StrUtil.isBlank(source)) {
                source = name;
            }
            Field field = ReflectionUtils.findField(clazz, source);
            if (Objects.isNull(field)) {
                log.warn("no field name is " + source);
                return value;
            }
            field.setAccessible(true);
            Object s = field.get(object);
            if (s == null) {
                return value;
            }
            Class<?> covert = attributes.getClass("covert");
            IFieldConvert bean = (IFieldConvert) beanFactory.getBean(covert);
            Assert.notNull(bean, "未获取到处理器的Bean");
            return bean.covert(s, name, attributes);
        } catch (Exception e) {
            log.error("convert field error, msg: " + e.getMessage(), e);
            return value;
        }
    }
}
