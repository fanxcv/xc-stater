package fun.fan.xc.plugin.auth;

import org.springframework.util.Assert;

/**
 * 认证工具类
 *
 * @author fan
 */
public class AuthLocal {
    private static final ThreadLocal<XcBaseUser> XC_USER_LOCAL = new ThreadLocal<>();

    private AuthLocal() {
    }

    public static XcBaseUser getUser() {
        return XC_USER_LOCAL.get();
    }

    public static void setUser(XcBaseUser user) {
        Assert.notNull(user, "user object is not be null");
        XC_USER_LOCAL.set(user);
    }

    public static void clearUser() {
        XC_USER_LOCAL.remove();
    }
}
