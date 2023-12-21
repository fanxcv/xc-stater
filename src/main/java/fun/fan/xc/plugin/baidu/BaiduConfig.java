package fun.fan.xc.plugin.baidu;

import fun.fan.xc.starter.utils.Dict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yangfan323
 */
@Data
@ConfigurationProperties(prefix = "xc.baidu")
public class BaiduConfig {
    /**
     * 百度云相关配置
     */
    private Cloud cloud;

    @Data
    public static class Cloud {
        private boolean enable = false;
        private String clientId = Dict.BLANK;
        private String clientSecret = Dict.BLANK;
    }
}
