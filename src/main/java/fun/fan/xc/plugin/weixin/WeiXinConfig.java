package fun.fan.xc.plugin.weixin;

import fun.fan.xc.starter.utils.Dict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author yangfan323
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "xc.weixin")
public class WeiXinConfig {
    /**
     * 公众号相关配置
     */
    private Official official;
    /**
     * 小程序相关配置
     */
    private MiniProgram miniProgram;
    /**
     * 服务端配置
     */
    private Server server;
    /**
     * 客户端配置
     */
    private Client client;

    @Data
    public static class Official {
        /**
         * 是否启用公众号接口
         */
        private boolean enable = false;
        /**
         * 微信服务器配置用到的Token
         */
        private String token = Dict.BLANK;
        /**
         * 微信公众号APP ID
         */
        private String appId = Dict.BLANK;
        /**
         * 微信公众号APP Secret
         */
        private String appSecret = Dict.BLANK;
    }

    @Data
    public static class MiniProgram {
        /**
         * 是否启用小程序接口
         */
        private boolean enable = false;
        /**
         * 微信小程序APP ID
         */
        private String appId = Dict.BLANK;
        /**
         * 微信小程序APP Secret
         */
        private String appSecret = Dict.BLANK;

        /**
         * 小程序支付相关
         */
        private Pay pay = new Pay();
    }

    @Data
    public static class Server {
        /**
         * 是否启用
         */
        private boolean enable = false;
        /**
         * 对外暴露接口的基础地址
         */
        private String basePath = WeiXinDict.WX_BASE_PATH;
        /**
         * 是否需要鉴权
         */
        private boolean enableAuth = true;
        /**
         * 鉴权用户配置
         */
        private List<Auth> authentication;
    }

    @Data
    public static class Client {
        /**
         * 是否启用
         */
        private boolean enable = false;
        /**
         * Server端请求地址
         */
        private String server;
        /**
         * 认证信息
         */
        private Auth authentication;
    }

    @Data
    public static class Auth {
        /**
         * 认证id
         */
        private String appId;
        /**
         * 认证secret
         */
        private String appSecret;
    }

    @Data
    public static class Pay {
        /**
         * V3-微信支付平台证书路径
         * @see "https://github.com/wechatpay-apiv3/CertificateDownloader"
         */
        private String wechatPayCertPath;
        /**
         * V3-商户证书序列号
         */
        private String apiKeySerialNo;
        /**
         * V3-商户API私钥路径
         */
        private String apiKeyPath;
        /**
         * 微信支付API秘钥地址, 必须
         * @see "https://pay.weixin.qq.com/docs/merchant/development/development-preparation/download-configure-merchant-certificates.html"
         */
        private String apiCertPath;
        /**
         * 微信支付V2Key
         * @see "https://pay.weixin.qq.com/docs/merchant/development/development-preparation/api-key-config.html"
         */
        private String apiV2Key;
        /**
         * V3-微信支付V3Key
         * @see "https://pay.weixin.qq.com/docs/merchant/development/development-preparation/api-key-config.html"
         */
        private String apiV3Key;
        /**
         * 商户id
         */
        private String mchId;
    }
}
