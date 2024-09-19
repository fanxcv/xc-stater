package fun.fan.xc.plugin.ca.mcs.annotations;


import java.lang.annotation.*;

/**
 * 用于标记接口地址, 相较于 {@link fun.fan.xc.plugin.ca.mcs.McsConfig#apiHost} 的路径
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McsPath {
    String value();
}
