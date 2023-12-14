package `fun`.fan.xc.starter.handler

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import `fun`.fan.xc.starter.annotation.VerifyParam
import `fun`.fan.xc.starter.event.EventImpl
import `fun`.fan.xc.starter.exception.ParamErrorException
import `fun`.fan.xc.starter.utils.Conversion
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.beans.factory.BeanFactory
import org.springframework.core.MethodParameter
import org.springframework.lang.NonNull
import org.springframework.util.StringUtils
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.lang.reflect.Type

/**
 * 注入相关参数
 */
class VerifyParamHandlerMethodArgumentResolver(beanFactory: BeanFactory) : BaseVerify(beanFactory),
    HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(VerifyParam::class.java)
    }

    override fun resolveArgument(
        @NonNull parameter: MethodParameter,
        modelAndViewContainer: ModelAndViewContainer?,
        @NonNull webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val event = EventImpl.getEvent()

        val verify = parameter.getParameterAnnotation(VerifyParam::class.java)!!

        // 获取参数名
        val name: Array<String> = verify.name

        if (name.size > 1) {
            throw ParamErrorException("name of VerifyParam can only have one value when in MethodParameter")
        }

        // VerifyParam拥有唯一注入对象，所以name只取第一个值
        val key: String = if (StringUtils.hasText(name[0])) name[0] else parameter.parameterName!!

        /*校验参数值，并做预处理*/
        var v = check(key, event.getParam(key), verify)

        val clazz = parameter.parameterType

        /*处理空参数*/
        v = handleNullValue(key, v, clazz)

        if (v == null) {
            return null
        }

        /*如果值是参数的子类，那就直接返回了*/
        if (clazz.isAssignableFrom(v.javaClass)) {
            // 如果待注入对象为List，并且获取到值也为数组对象
            if (v.javaClass == JSONArray::class.java) {
                val type: Type = parameter.genericParameterType
                v = (v as JSONArray).to(type)
            } else if (v.javaClass == JSONObject::class.java) {
                val type: Type = parameter.genericParameterType
                v = (v as JSONObject).to(type)
            }
            return v
        }

        return try {
            Conversion.binder.convertIfNecessary(v, clazz, parameter)
        } catch (ex: ConversionNotSupportedException) {
            throw MethodArgumentConversionNotSupportedException(
                v, ex.requiredType,
                key, parameter, ex.cause!!
            )
        } catch (ex: TypeMismatchException) {
            throw MethodArgumentTypeMismatchException(
                v, ex.requiredType,
                key, parameter, ex.cause!!
            )
        }
    }

    private fun handleNullValue(name: String, value: Any?, paramType: Class<*>): Any? {
        if (value == null) {
            if (java.lang.Boolean.TYPE == paramType) {
                return java.lang.Boolean.FALSE
            } else if (paramType.isPrimitive) {
                throw ParamErrorException(
                    "Optional ${paramType.simpleName} parameter '$name" +
                            "' is present but cannot be translated into a null value due to being declared as a " +
                            "primitive type. Consider declaring it as object wrapper for the corresponding primitive type."
                )
            }
        }
        return value
    }
}
