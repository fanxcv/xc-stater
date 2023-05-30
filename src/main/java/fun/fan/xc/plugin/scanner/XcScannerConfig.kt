package `fun`.fan.xc.plugin.scanner

import cn.hutool.core.util.ArrayUtil
import com.google.common.collect.Maps
import `fun`.fan.xc.starter.utils.BeanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.StandardAnnotationMetadata
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import java.lang.reflect.Field
import java.util.*

/**
 * @author fan
 */
class XcScannerConfig : ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanFactoryAware {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private var resourceLoader: ResourceLoader? = null
    private var beanFactory: BeanFactory? = null

    override fun registerBeanDefinitions(annotationMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        val attributes = annotationMetadata.getAnnotationAttributes(
            XcScan::class.java.name
        )
        if (Objects.isNull(attributes)) {
            log.warn("get XcScan attributes failed")
            return
        }
        var basePackages = attributes!!["basePackages"] as Array<String?>
        if (ArrayUtil.isEmpty(basePackages)) {
            // 如果没有指定需要扫描的包, 则默认启动类所在的包
            // basePackages = arrayOf((annotationMetadata as StandardAnnotationMetadata).introspectedClass.packageName)
        }
        val matches = attributes["matches"] as Array<AnnotationAttributes>
        if (ArrayUtil.isEmpty(matches)) {
            log.warn("scanner types is empty")
            return
        }
        val cache: MutableMap<Class<*>, XcScannerConsumer> = Maps.newHashMap()
        for (match in matches) {
            val types = match.getClassArray("types")
            val consumer = match.getClass<Any>("consumer")
            for (type in types) {
                if (!registry.containsBeanDefinition(consumer.name)) {
                    // 初始化对应的处理器
                    val beanDefinition = RootBeanDefinition(consumer)
                    registry.registerBeanDefinition(consumer.name, beanDefinition)
                }
                val bean = beanFactory!!.getBean(consumer) as XcScannerConsumer
                cache[type] = bean
            }
        }
        val scanner = XcClassPathBeanDefinitionScanner(registry, cache)
        scanner.setResourceLoader(resourceLoader)
        scanner.scan(*basePackages)
    }

    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    @Throws(BeansException::class)
    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    class XcClassPathBeanDefinitionScanner(registry: BeanDefinitionRegistry?, cache: Map<Class<*>, XcScannerConsumer>) :
        ClassPathBeanDefinitionScanner(
            registry!!, false
        ) {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
        private val cache: Map<Class<*>, XcScannerConsumer>
        private val scannerTypes: Set<Class<*>>

        init {
            scannerTypes = cache.keys
            this.cache = cache
        }

        override fun doScan(vararg basePackages: String): Set<BeanDefinitionHolder> {
            // 扫描包下所有的类
            addIncludeFilter { _: MetadataReader?, _: MetadataReaderFactory? -> true }
            return super.doScan(*basePackages)
        }

        override fun postProcessBeanDefinition(beanDefinition: AbstractBeanDefinition, beanName: String) {
            super.postProcessBeanDefinition(beanDefinition, beanName)
            val className = beanDefinition.beanClassName
            val clazz: Class<*> = try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                log.error("load class [$className] error")
                return
            }
            val fields: List<Field> = BeanUtils.getAllFields(clazz, false)
            for (field in fields) {
                val annotations = field.annotations
                if (ArrayUtil.isEmpty(annotations)) {
                    continue
                }
                for (annotation in annotations) {
                    val a: Class<out Annotation> = annotation.annotationClass.javaObjectType
                    if (!scannerTypes.contains(a)) {
                        continue
                    }
                    val consumer = cache[a]
                    consumer!!.accept(clazz, field, annotation)
                }
            }
        }

        override fun registerBeanDefinition(definitionHolder: BeanDefinitionHolder, registry: BeanDefinitionRegistry) {
            // skip register
        }
    }
}