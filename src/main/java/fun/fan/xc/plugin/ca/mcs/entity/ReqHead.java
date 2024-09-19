package fun.fan.xc.plugin.ca.mcs.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ReqHead {
    /**
     * 应用Id
     */
    private String appId;

    /**
     * 商户编码
     */
    private String customerId;

    /**
     * 交互协议的版本号
     */
    private String version;

    /**
     * 请求时间
     */
    private String reqTime;

    /**
     * 业务流水号
     */
    private String serialNo;

    /**
     * 透传参数
     */
    private String attach;
}
