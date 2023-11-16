package fun.fan.xc.plugin.auth;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Objects;

/**
 * 认证工具类
 *
 * @author fan
 */
public class AuthLocal {
    private static final ThreadLocal<XcBaseUser> XC_USER_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Collection<String>> XC_PERMISSION_LOCAL = new ThreadLocal<>();

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
        clearPermissions();
    }

    public static Collection<String> getPermissions() {
        return XC_PERMISSION_LOCAL.get();
    }

    public static void setPermissions(Collection<String> permissions) {
        if (Objects.nonNull(permissions)) {
            XC_PERMISSION_LOCAL.set(permissions);
        }
    }

    public static void clearPermissions() {
        XC_PERMISSION_LOCAL.remove();
    }
}
