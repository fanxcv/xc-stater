package fun.fan.xc.plugin.auth.interceptor;

import fun.fan.xc.plugin.auth.AuthConfigure;
import fun.fan.xc.plugin.auth.AuthConstant;
import fun.fan.xc.plugin.auth.XcBaseUser;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理端认证拦截器
 *
 * @author fan
 */
public interface XcAuthInterceptor {

    /**
     * 返回客户端名称
     */
    default String client() {
        return AuthConstant.DEFAULT_CLIENT;
    }

    /**
     * 获取token的方法
     *
     * @param request request
     * @return token
     */
    default String getToken(AuthConfigure.Configure configure, HttpServletRequest request) {
        return request.getHeader(configure.getTokenName());
    }

    /**
     * 检查用户的有效性和权限
     *
     * @param user 待校验的用户对象
     * @return 校验结果
     */
    default boolean checkUser(XcBaseUser user){
        return true;
    }

    /**
     * 获取客户端配置
     * @return 配置对象
     */
    default AuthConfigure.Configure getConfigure(AuthConfigure configure) {
        return configure.getConfigureByClient(this.client());
    }
}
