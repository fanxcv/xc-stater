package fun.fan.xc.plugin.ca.mcs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yangfan323
 */
@Data
@ConfigurationProperties(prefix = "xc.ca.mcs")
public class McsConfig {
    private boolean debug;
    /**
     * 接口地址
     */
    private String apiHost;
    /**
     * 版本
     */
    private String version = "1.0.0";
    /**
     * 签约机构应用ID
     */
    private String appId;
    /**
     * 签约机构编码
     */
    private String customerId;
    /**
     * 机构私钥
     */
    private String privateKey;
    /**
     * 机构公钥
     */
    private String publicKey;
}
