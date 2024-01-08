package `fun`.fan.xc.plugin.gateway

import org.springframework.web.method.HandlerMethod
import jakarta.servlet.http.HttpServletRequest

/**
 * Gateway处理器接口
 * @author fan
 */
interface XcGatewayHandler {
    /**
     * 校验请求是否符合预期
     * @param handler Spring处理器
     * @param request 当前的请求对象
     * @return 如果为false, 会直接终止请求
     */
    fun check(handler: HandlerMethod, request: HttpServletRequest): Boolean
}
