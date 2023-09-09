package fun.fan.xc.plugin.auth;

import org.springframework.util.Assert;

/**
 * 认证工具类
 *
 * @author fan
 */
public class AuthLocal {
    private AuthLocal() {
    }

    private static final ThreadLocal<XcBaseUser> THREAD_LOCAL = new ThreadLocal<>();

    public static XcBaseUser getUser() {
        return THREAD_LOCAL.get();
    }

    public static void setUser(XcBaseUser user) {
        Assert.notNull(user, "user object is not be null");
        THREAD_LOCAL.set(user);
    }

    public static void clearUser() {
        THREAD_LOCAL.remove();
    }
}
