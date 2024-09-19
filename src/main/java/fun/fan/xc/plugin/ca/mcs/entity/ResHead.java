package fun.fan.xc.plugin.ca.mcs.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResHead {
    /**
     * 应用Id
     */
    private String appId;

    /**
     * 请求时间
     */
    private String reqTime;

    /**
     * 返回时间
     */
    private String resTime;

    /**
     * 返回码 成功码：200
     */
    private String code;

    /**
     * 消息
     */
    private String message;

    /**
     * 签约机构ID
     */
    private String customerId;

    /**
     * 透传参数
     */
    private String attach;
}
