package fun.fan.xc.plugin.auth.service;

import fun.fan.xc.plugin.auth.XcBaseUser;

/**
 * 认证用户相关服务
 *
 * @author fan
 */
public interface XcAuthUserService {
    /**
     * 通过accountId查询User对象服务
     *
     * @param account accountId
     * @return user对象
     */
    XcBaseUser select(String client, String account);
}
