package fun.fan.xc.plugin.auth;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * 管理端认证拦截器
 *
 * @author fan
 */
public interface XcAuthInterface {

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
    default boolean checkUser(XcBaseUser user) {
        return true;
    }

    /**
     * 获取客户端配置
     *
     * @return 配置对象
     */
    default AuthConfigure.Configure getConfigure(AuthConfigure configure) {
        return configure.getConfigureByClient(this.client());
    }

    /**
     * 获取权限列表
     *
     * @param user 当前用户
     * @return 权限列表
     */
    default Set<String> selectPermissions(XcBaseUser user) {
        return null;
    }

    /**
     * 通过accountId查询User对象服务
     *
     * @param account accountId
     * @return user对象
     */
    XcBaseUser select(String account);
}
