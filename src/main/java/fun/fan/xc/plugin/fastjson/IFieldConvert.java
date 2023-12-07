package fun.fan.xc.plugin.fastjson;

import org.springframework.core.annotation.AnnotationAttributes;

/**
 * 字段映射接口
 *
 * @author fan
 */
public interface IFieldConvert {
    /**
     * 转换方法
     *
     * @param source     原始值
     * @param name       注解标注字段名
     * @param attributes 注解属性
     * @return 新值
     * @throws Exception 错误
     */
    Object covert(Object source, String name, AnnotationAttributes attributes) throws Exception;
}
