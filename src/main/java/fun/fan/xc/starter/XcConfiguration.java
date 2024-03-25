package fun.fan.xc.starter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author fan
 */
@Data
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(XcConfiguration.PREFIX)
public class XcConfiguration {
    public static final String PREFIX = "xc";

    /**
     * 核心配置
     */
    private CoreConfig core = new CoreConfig();

    /**
     * 跨域配置
     */
    private CorsConfig cors = new CorsConfig();
    /**
     * Drone配置
     */
    private DroneConfig drone = new DroneConfig();

    /**
     * 网关配置
     */
    private GatewayConfig gateway;

    @Data
    public static class CoreConfig {
        /**
         * core拦截路径
         */
        private List<String> path = Lists.newArrayList("/**");
        /**
         * core拦截器排除路径
         */
        private Set<String> excludePath = Sets.newHashSet("/error");

        /**
         * 是否启用core
         */
        private boolean enable = true;

        public List<String> getExcludePath() {
            return Lists.newArrayList(this.excludePath);
        }

        public void setExcludePath(List<String> excludePath) {
            this.excludePath.addAll(excludePath);
        }
    }

    @Data
    public static class CorsConfig {
        private transient List<String> any = Collections.singletonList("*");
        /**
         * Origin白名单
         */
        private List<String> allowedOrigins = any;
        /**
         * Header白名单
         */
        private List<String> allowedHeaders = any;
        /**
         * Method白名单
         */
        private List<String> allowedMethods = any;
        /**
         * 处理路径路径
         */
        private List<String> path = Lists.newArrayList("/**");
    }

    @Data
    public static class DroneConfig {
        /**
         * Api 接口地址
         */
        private String host;
        /**
         * Api Token
         */
        private String token;
        /**
         * Git 用户名
         */
        private String gitUser;
        /**
         * Git 密码
         */
        private String gitPassword;
    }

    @Data
    public static class GatewayConfig {
        /**
         * 白名单, 支持细化配置
         */
        private List<IpCheck> whiteIps = Lists.newArrayList();
        /**
         * 黑名单, 仅支持全局生效
         */
        private List<String> blackIps;
    }

    @Data
    public static class IpCheck {
        /**
         * 需要校验的路径，支持通配
         */
        private String path;
        /**
         * 允许的IP列表，支持cidr
         */
        private List<String> ips = Lists.newArrayList();
    }
}
