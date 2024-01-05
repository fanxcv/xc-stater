package `fun`.fan.xc.plugin.gateway

import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit

/**
 * @author fan
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CLASS)
annotation class ApiCheck(
    /**
     * 请求间隔配置
     */
    val timeLimit: TimeLimit = TimeLimit(),
    /**
     * 请求频率配置
     */
    val rateLimit: Array<RateLimit> = [],
    /**
     * 滑动窗口限流配置
     */
    val rollLimit: Array<RollLimit> = [],
    /**
     * 是否对接口不做访问限制
     */
    val unCheck: Boolean = false,
    /**
     * 是否可以使用Token校验访问
     * 默认关闭
     */
    val useToken: Boolean = false
) {
    @Target
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TimeLimit(
        /**
         * 等待时间
         */
        val value: Long = -1,
        /**
         * 等待时间单位
         */
        val time: TimeUnit = TimeUnit.SECONDS
    )

    @Target
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RateLimit(
        /**
         * 周期允许次数
         */
        val value: Long = -1,
        /**
         * 时间周期
         */
        val time: Long = 1,
        /**
         * 周期时间单位
         */
        val timeUnit: TimeUnit = TimeUnit.SECONDS
    )

    @Target
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RollLimit(
        /**
         * 请求次数
         */
        val value: Long = -1,
        /**
         * 窗口个数
         */
        val window: Long = 10,
        /**
         * 时间
         */
        val time: Long = 1,
        /**
         * 时间单位
         */
        val timeUnit: TimeUnit = TimeUnit.SECONDS,
        /**
         * 缓存Key,支持EL表达式,可以从入参中获取值#{#param}
         */
        val key: String,
        /**
         * 当key在match里的话才执行限流
         * 如果match为空也执行限流
         */
        val match: Array<String> = []
    )
}
