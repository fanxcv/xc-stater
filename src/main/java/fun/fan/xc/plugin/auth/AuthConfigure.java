package fun.fan.xc.plugin.auth;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fun.fan.xc.starter.exception.XcServiceException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author fan
 */
@Data
@ConfigurationProperties("xc")
public class AuthConfigure {
    private Map<String, Configure> auth;

    @Data
    public static class Configure {
        /**
         * 需要拦截的请求路径
         */
        private List<String> path = Lists.newArrayList("/**");
        /**
         * 拦截器排除路径
         */
        private Set<String> excludePath = Sets.newHashSet(AuthConstant.BASE_EXCLUDE_PATH);
        /**
         * Token Header名
         */
        private String tokenName = "X-Token";
        /**
         * Token有效期，单位分
         */
        private int expires = 120;
        /**
         * 用户缓存时间，单位分
         */
        private int userCacheExpires = 30;
        /**
         * 同时允许在线用户数, 0为不限制
         */
        private int allowedOnline = 0;
    }

    /**
     * 不同端获取相应的配置
     */
    public Configure getConfigureByClient(String client) {
        return Optional.ofNullable(auth.get(client)).orElseThrow(() -> new XcServiceException("未找到配置"));
    }
}
