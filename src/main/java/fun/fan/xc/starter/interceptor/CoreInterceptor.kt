package `fun`.fan.xc.starter.interceptor

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import `fun`.fan.xc.plugin.gateway.XcGatewayHandler
import `fun`.fan.xc.starter.enums.ReturnCode
import `fun`.fan.xc.starter.event.EventImpl
import `fun`.fan.xc.starter.event.EventInner
import `fun`.fan.xc.starter.exception.XcRunException
import `fun`.fan.xc.starter.interfaces.XcEventInterface
import `fun`.fan.xc.starter.utils.Dict
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.http.MediaType
import org.springframework.util.StringUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.nio.charset.Charset
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * 核心拦截器, 主要处理请求参数
 */
class CoreInterceptor(applicationContext: ApplicationContext) : HandlerInterceptor {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val type = object : ParameterizedTypeReference<Map<String?, Any?>?>() {}.type

    private val gatewayHandler: Collection<XcGatewayHandler>? = try {
        val vs = applicationContext.getBeansOfType(XcGatewayHandler::class.java).values.toMutableList()
        AnnotationAwareOrderComparator.sort(vs)
        vs
    } catch (e: Exception) {
        log.error("no instance of XcGatewayHandler", e)
        null
    }

    private val eventInterface: XcEventInterface? = try {
        applicationContext.getBean(XcEventInterface::class.java)
    } catch (e: NoSuchBeanDefinitionException) {
        log.warn("no instance of XcEventInterface, don't init it")
        null
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler is HandlerMethod) {
            val event = EventImpl.instance()
            // 解析参数
            getRequestParam(request, event)
            // 网关拦截器
            gatewayHandler?.forEach { if (!it.check(handler, request)) throw XcRunException(ReturnCode.FORBIDDEN) }

            // 标记该请求有使用XcCore处理
            request.setAttribute(Dict.REQUEST_DEAL_BY_XC_CORE, true)
        }
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: java.lang.Exception?
    ) {
        val event = EventImpl.getEvent()
        // 请求完成后需要初始化event对象
        event.init()
    }

    private fun getRequestParam(request: HttpServletRequest, input: EventInner) {
        // 先直接获取参数
        request.parameterMap?.forEach { (k: String, v: Array<String>?) ->
            input.putParam(
                k,
                if (v.size == 1) v[0] else listOf(*v)
            )
        }
        // 处理json和xml两种数据
        val contentType = request.contentType
        if (StringUtils.hasText(contentType)) {
            // 尝试读取请求消息体，如果获取到了消息体，尝试读取里面的参数
            try {
                dealRequestBody(input, request, contentType)
            } catch (e: Exception) {
                log.error("params parse error: ${e.message}")
            }
        }
    }

    private fun dealRequestBody(input: EventInner, request: HttpServletRequest, contentType: String) {
        if (contentType.contains(MediaType.APPLICATION_JSON_VALUE)) { // 处理Json
            val map: Map<String, Any?> = JSON.parseObject(
                request.inputStream,
                Charset.forName(request.characterEncoding),
                type, JSONReader.Feature.UseBigDecimalForDoubles
            )
            input.putParamAll(map)
        } else if (contentType.contains(MediaType.APPLICATION_XML_VALUE)) { // 处理xml数据
            log.warn("not support xml parse")
            // Map<String, String> map = XmlTools.xml2Map(line);
            // input.putParamAll(map);
        }
    }
}
