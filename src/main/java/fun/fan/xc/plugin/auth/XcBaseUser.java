package fun.fan.xc.plugin.auth;

import java.util.Collection;

/**
 * 用户基础接口
 *
 * @author fan
 */
public interface XcBaseUser {

    /**
     * 获取用户唯一标识, 可以是id, 也可以是账号
     */
    String getAccount();

    /**
     * 获取用户名
     */
    String getName();

    /**
     * 获取用户角色列表
     */
    default Collection<String> getRoles() {
        return null;
    }

    /**
     * 获取用户Token
     */
    String getToken();

    void setToken(String token);

    default String getClient() {
        return AuthConstant.DEFAULT_CLIENT;
    }

    void setClient(String client);
}
