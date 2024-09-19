package fun.fan.xc.plugin.ca.mcs.entity;

import lombok.Data;

@Data
public class McsBody {
    /**
     * 业务流水号
     */
    private String serialNo;

    /**
     * 透传参数
     */
    private String attach;
}
