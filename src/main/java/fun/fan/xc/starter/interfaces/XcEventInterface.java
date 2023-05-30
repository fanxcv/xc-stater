package fun.fan.xc.starter.interfaces;


import javax.servlet.http.HttpServletRequest;

/**
 * Core 对外暴露接口
 *
 * @author fan
 */
public interface XcEventInterface {

    /**
     * 从请求中解析Token
     *
     * @param request 当前请求对象
     * @return token
     */
    default String parseToken(HttpServletRequest request) {
        return null;
    }
}
