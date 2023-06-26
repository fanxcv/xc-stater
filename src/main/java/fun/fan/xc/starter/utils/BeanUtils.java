package fun.fan.xc.starter.utils;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import fun.fan.xc.starter.exception.XcRunException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.util.CollectionUtils;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 实体转换工具类
 *
 * @author fan
 */
@Slf4j
public class BeanUtils {

    /**
     * 缓存BeanCopier 对象 提升性能
     */
    private static final Map<String, BeanCopier> BEAN_COPIER_MAP = new ConcurrentHashMap<>();
    /**
     * 缓存bean属性
     */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     *
     * @param source      需要转换的Bean
     * @param destination 需要转换成的Bean, source的同名属性会覆盖到destination的属性
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, D destination) {
        return beanToBean(source, destination, null, null);
    }


    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     *
     * @param source 需要转换的Bean
     * @param clazz  需要转换成的Bean的Class
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, Class<D> clazz) {
        return beanToBean(source, clazz, null, null);
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source      需要转换的Bean
     * @param destination 需要转换成的Bean, source的同名属性会覆盖到destination的属性
     * @param mapValues   用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, D destination, Map<String, Function<S, Object>> mapValues) {
        return beanToBean(source, destination, null, mapValues);
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source    需要转换的Bean
     * @param clazz     需要转换成的Bean的Class
     * @param mapValues 用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, Class<D> clazz, Map<String, Function<S, Object>> mapValues) {
        return beanToBean(source, clazz, null, mapValues);
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source      需要转换的Bean
     * @param destination 需要转换成的Bean, source的同名属性会覆盖到destination的属性
     * @param ignore      忽略的字段
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, D destination, String[] ignore) {
        return beanToBean(source, destination, ignore, null);
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source 需要转换的Bean
     * @param clazz  需要转换成的Bean的Class
     * @param ignore 忽略的字段
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, Class<D> clazz, String[] ignore) {
        return beanToBean(source, clazz, ignore, null);
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source      需要转换的Bean
     * @param destination 需要转换成的Bean, source的同名属性会覆盖到destination的属性
     * @param ignore      忽略的字段
     * @param mapValues   用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, D destination, String[] ignore, Map<String, Function<S, Object>> mapValues) {
        if (Objects.isNull(source) || Objects.isNull(destination)) {
            return destination;
        }

        try {
            Class<?> oClazz = source.getClass();
            Class<?> clazz = destination.getClass();

            List<Field> oFields = getAllFields(oClazz, true);
            List<Field> fields = getAllFields(clazz, true);

            if (CollectionUtils.isEmpty(fields) || CollectionUtils.isEmpty(oFields)) {
                return destination;
            }
            // 处理忽略字段
            Map<String, String> map = new HashMap<>(16);
            if (ArrayUtils.isNotEmpty(ignore)) {
                for (String s : ignore) {
                    map.put(s, s);
                }
            }
            for (Field field : fields) {
                // 获取字段名
                String name = field.getName();
                // 处理忽略字段
                if (Objects.nonNull(map.get(name))) {
                    continue;
                }
                // 获取值,先尝试从映射表中获取,获取不到的话尝试从原始数据类获取
                Function<S, Object> function = Optional.ofNullable(mapValues).map(it -> it.get(name)).orElse(null);
                Object res = function != null ? function.apply(source) : tryGetSourceValue(source, oFields, name);

                if (Objects.isNull(res)) {
                    continue;
                }

                // 尝试转换下参数对象
                res = Conversion.getBinder().convertIfNecessary(res, field.getType());
                // 设置值
                field.setAccessible(true);
                field.set(destination, res);
            }
            return destination;
        } catch (Exception e) {
            throw new RuntimeException("copy bean fail: " + e.getMessage(), e);
        }
    }

    /**
     * Bean的转换，支持map转bean,不支持bean转map
     * 以需要转换的bean字段为基础, 从源对象提取数据
     * 通过mapValues预设的转换规则, 可以做数据转换或字段映射
     *
     * @param source    需要转换的Bean
     * @param clazz     需要转换成的Bean的Class
     * @param ignore    忽略的字段
     * @param mapValues 用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的对象
     */
    public static <S, D> D beanToBean(S source, Class<D> clazz, String[] ignore, Map<String, Function<S, Object>> mapValues) {
        if (source == null || clazz == null) {
            return null;
        }

        try {
            D t = clazz.getDeclaredConstructor().newInstance();
            // 仅当两个都有属性的时候才遍历
            return beanToBean(source, t, ignore, mapValues);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("copy bean fail");
        }
    }

    /**
     * 转换list
     *
     * @param source     源数据
     * @param createDest 创建目标对象
     * @return 转换成功的List
     */
    public static <S, D> List<D> convertList(Collection<S> source, Supplier<D> createDest) {
        return convertList(source, createDest, null, null);
    }

    /**
     * 转换list
     *
     * @param source     源数据
     * @param createDest 创建目标对象
     * @param ignore     忽略的字段
     * @return 转换成功的List
     */
    public static <S, D> List<D> convertList(Collection<S> source, Supplier<D> createDest, String[] ignore) {
        return convertList(source, createDest, ignore, null);
    }

    /**
     * 转换list
     *
     * @param source     源数据
     * @param createDest 创建目标对象
     * @param mapValues  用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的List
     */
    public static <S, D> List<D> convertList(Collection<S> source, Supplier<D> createDest, Map<String, Function<S, Object>> mapValues) {
        return convertList(source, createDest, null, mapValues);
    }

    /**
     * 转换list
     *
     * @param source     源数据
     * @param createDest 创建目标对象
     * @param ignore     忽略的字段
     * @param mapValues  用于转换字段值,Function入参为原始对象,出参为新字段值
     * @return 转换成功的List
     */
    public static <S, D> List<D> convertList(Collection<S> source, Supplier<D> createDest, String[] ignore, Map<String, Function<S, Object>> mapValues) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        return source.stream()
                .map(it -> beanToBean(it, createDest.get(), ignore, mapValues))
                .collect(Collectors.toList());
    }

    private static <S> Object tryGetSourceValue(S source, List<Field> oFields, String name) {
        // 如果source是map
        if (Map.class.isAssignableFrom(source.getClass())) {
            // 先尝试用原始字段名获取属性, 获取不到的话尝试将驼峰转为下划线后再尝试获取
            Object o = ((Map<?, ?>) source).get(name);
            if (Objects.isNull(o)) {
                return ((Map<?, ?>) source).get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name));
            } else {
                return o;
            }
        } else {
            return oFields.stream()
                    .filter(it -> name.equals(it.getName()))
                    .findFirst()
                    .map(it -> {
                        it.setAccessible(true);
                        try {
                            return it.get(source);
                        } catch (IllegalAccessException e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }
    }

    /**
     * 获取所有属性,包括父类的
     *
     * @param clazz    待获取的class
     * @param useCache 是否走cache
     * @return 属性列表
     */
    public static List<Field> getAllFields(Class<?> clazz, boolean useCache) {
        return Optional.ofNullable(useCache ? FIELD_CACHE_MAP.get(clazz) : null)
                .orElseGet(() -> {
                    Map<String, Field> map = new HashMap<>(16);
                    getAllFields(clazz, map);
                    LinkedList<Field> fields = Lists.newLinkedList(map.values());
                    if (useCache) {
                        FIELD_CACHE_MAP.put(clazz, fields);
                    }
                    return fields;
                });
    }

    /**
     * 递归实现,通过栈实现子类属性覆盖父类属性
     *
     * @param clazz 待解析的class
     * @param map   存放field的map
     */
    private static void getAllFields(Class<?> clazz, Map<String, Field> map) {
        Class<?> c = clazz.getSuperclass();
        if (clazz != Object.class && c != null) {
            getAllFields(c, map);
        }
        // 读取所有的属性
        Field[] fields = clazz.getDeclaredFields();
        if (ArrayUtils.isEmpty(fields)) {
            return;
        }
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            // 跳过static或final修饰的属性
            if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                continue;
            }
            map.put(field.getName(), field);
        }
    }


    /**
     * Bean属性复制工具方法。
     *
     * @param sources   原始集合
     * @param supplier: 目标类::new(eg: UserVO::new) 避免class每次反射创建对象
     */
    public static <S, T> List<T> copyList(List<S> sources, Supplier<T> supplier) {
        if (CollectionUtils.isEmpty(sources)) {
            return Collections.emptyList();
        }
        List<T> list = new ArrayList<>(sources.size());
        BeanCopier beanCopier = null;
        for (S source : sources) {
            T t = supplier.get();
            if (beanCopier == null) {
                beanCopier = getBeanCopier(source.getClass(), t.getClass());
            }
            beanCopier.copy(source, t, null);
            list.add(t);
        }
        return list;
    }

    /**
     * Bean属性复制工具方法。
     *
     * @param source    目标对象
     * @param supplier: 目标类::new(eg: UserVO::new)
     */
    public static <T> T copyBean(Object source, Supplier<T> supplier) {
        T t = supplier.get();
        getBeanCopier(source.getClass(), t.getClass()).copy(source, t, null);
        return t;
    }


    /**
     * 获取BeanCopier对象 如果缓存中有从缓存中获取 如果没有则新创建对象并加入缓存
     *
     * @param sourceClass 目标对象
     * @param targetClass 目标类
     * @return BeanCopier
     */
    private static BeanCopier getBeanCopier(Class<?> sourceClass, Class<?> targetClass) {
        String key = getKey(sourceClass.getName(), targetClass.getName());
        BeanCopier beanCopier;
        beanCopier = BEAN_COPIER_MAP.get(key);
        if (beanCopier == null) {
            beanCopier = BeanCopier.create(sourceClass, targetClass, false);
            BEAN_COPIER_MAP.put(key, beanCopier);
        }
        return beanCopier;
    }

    /**
     * 生成缓存key
     */
    private static String getKey(String sourceClassName, String targetClassName) {
        return sourceClassName + targetClassName;
    }

    /**
     * bean转map值
     *
     * @param bean 运单明细信息bean
     */
    public static Map<String, Object> beanToMap(Object bean) {
        return beanToMap(bean, null);
    }

    /**
     * bean转map值
     * 允许使用自定义方式获取key
     *
     * @param bean 运单明细信息bean
     */
    public static Map<String, Object> beanToMap(Object bean,
                                                Function<Field, String> keyFunction) {
        Map<String, Object> wrapper = new HashMap<>(16);
        if (bean == null) {
            return wrapper;
        }
        List<Field> fields = getAllFields(bean.getClass(), true);
        fields.forEach(field -> {
            String key;
            if (Objects.nonNull(keyFunction)) {
                key = keyFunction.apply(field);
            } else {
                key = field.getName();
            }
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(bean);
            } catch (IllegalAccessException e) {
                throw new XcRunException(e);
            }
            wrapper.put(key, value);
        });
        return wrapper;
    }
}
