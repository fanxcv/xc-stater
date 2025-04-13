package fun.fan.xc.plugin.meituan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "xc.meituan")
public class MtConfig {
    /**
     * 美团开发者ID
     */
    private Long developerId;
    /**
     * 美团开发者密钥
     */
    private String signKey;
    /**
     * 业务ID
     */
    private Integer businessId;
}
