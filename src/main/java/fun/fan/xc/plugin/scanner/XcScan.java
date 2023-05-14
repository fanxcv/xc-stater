package fun.fan.xc.plugin.scanner;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(XcScannerConfig.class)
public @interface XcScan {
    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};

    /**
     * 扫描注解配置
     */
    Matcher[] matches() default {};

    @interface Matcher {
        /**
         * 需要扫描的注解
         */
        @AliasFor("types")
        Class<?>[] value() default {};

        /**
         * 需要扫描的注解
         */
        @AliasFor("value")
        Class<?>[] types() default {};

        Class<? extends XcScannerConsumer> consumer();
    }
}
