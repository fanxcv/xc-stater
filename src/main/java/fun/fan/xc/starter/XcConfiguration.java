package fun.fan.xc.starter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author fan
 */
@Setter
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("xc")
public class XcConfiguration {
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

    @Setter
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

        public List<String> getPath() {
            return path;
        }

        public List<String> getExcludePath() {
            return Lists.newArrayList(this.excludePath);
        }

        public void setExcludePath(List<String> excludePath) {
            this.excludePath.addAll(excludePath);
        }

        public boolean isEnable() {
            return enable;
        }
    }

    @Setter
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

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public List<String> getPath() {
            return path;
        }
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

        public String getHost() {
            return host;
        }

        public String getToken() {
            return token;
        }

        public String getGitUser() {
            return gitUser;
        }

        public String getGitPassword() {
            return gitPassword;
        }
    }

    public CoreConfig getCore() {
        return core;
    }

    public CorsConfig getCors() {
        return cors;
    }

    public DroneConfig getDrone() {
        return drone;
    }
}
