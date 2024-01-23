package fun.fan.xc.plugin.upload;

import fun.fan.xc.starter.utils.Dict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author fan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "xc.upload")
public class UploadConfig {
    /**
     * minio客户端配置
     */
    private MinioConfig minio;

    @Data
    public static class MinioConfig {
        private boolean enable = true;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        /**
         * 用于构建访问url的基础路径
         */
        private String baseHost = Dict.BLANK;
        /**
         * 默认使用的桶名称
         */
        private String bucketName;
    }
}
