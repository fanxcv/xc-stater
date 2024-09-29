package fun.fan.xc.plugin.baidu.cloud.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BaseResponse {
    @JSONField(name = "error_code")
    @JsonProperty("error_code")
    private String errorCode;

    @JSONField(name = "error_msg")
    @JsonProperty("error_msg")
    private String errorMsg;
}
