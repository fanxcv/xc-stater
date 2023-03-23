package `fun`.fan.xc.starter.converters

import com.alibaba.fastjson2.JSON
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.ConditionalGenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair

/**
 * @author fan
 */
class String2ListGenericConverter : ConditionalGenericConverter {
    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean = true

    override fun getConvertibleTypes(): Set<ConvertiblePair> {
        return setOf(ConvertiblePair(String::class.java, MutableList::class.java))
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        if (source == null) return null
        val string = source as String
        val elementTypeDescriptor = targetType.elementTypeDescriptor
        return if (elementTypeDescriptor == null) {
            JSON.parseArray(string)
        } else {
            JSON.parseArray(string, elementTypeDescriptor.type)
        }
    }
}