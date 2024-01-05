package `fun`.fan.xc.plugin.gateway

import java.lang.annotation.Inherited

/**
 * 为Api添加分组,后面可以通过为该组设置规则来控制该组下下的所有接口权限
 * @author fan
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ApiGroup(
    /**
     * group名字
     */
    vararg val value: String
)
