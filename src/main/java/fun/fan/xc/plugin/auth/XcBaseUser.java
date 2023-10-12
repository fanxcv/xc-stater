package fun.fan.xc.plugin.auth;

/**
 * 用户基础接口
 *
 * @author fan
 */
public interface XcBaseUser {

    String getAccount();

    String getName();

    String getToken();

    void setToken(String token);

    default String getClient() {
        return AuthConstant.DEFAULT_CLIENT;
    }

    void setClient(String client);
}
