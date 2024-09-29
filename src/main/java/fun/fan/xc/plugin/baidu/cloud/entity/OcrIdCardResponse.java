package fun.fan.xc.plugin.baidu.cloud.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * { @link "<a href="https://cloud.baidu.com/doc/OCR/s/rk3h7xzck">文档地址</a>" }
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class OcrIdCardResponse extends BaseResponse {
    @JSONField(name = "log_id")
    @JsonProperty("log_id")
    private long logId;

    @JSONField(name = "words_result")
    @JsonProperty("words_result")
    private Map<String, Location> wordsResult;

    @JSONField(name = "words_result_num")
    @JsonProperty("words_result_num")
    private int wordsResultNum;

    @JSONField(name = "direction")
    @JsonProperty("direction")
    private Integer direction;

    @JSONField(name = "image_status")
    @JsonProperty("image_status")
    private String imageStatus;

    @JSONField(name = "card_ps")
    @JsonProperty("card_ps")
    private String cardPs;

    @JSONField(name = "risk_type")
    @JsonProperty("risk_type")
    private String riskType;

    @JSONField(name = "edit_tool")
    @JsonProperty("edit_tool")
    private String editTool;

    @JSONField(name = "card_quality")
    @JsonProperty("card_quality")
    private CardQuality cardQuality;

    @JSONField(name = "photo")
    @JsonProperty("photo")
    private String photo;

    @JSONField(name = "photo_location")
    @JsonProperty("photo_location")
    private Location photoLocation;

    @JSONField(name = "card_image")
    @JsonProperty("card_image")
    private String cardImage;

    @JSONField(name = "card_location")
    @JsonProperty("card_location")
    private Location cardLocation;

    @JSONField(name = "idcard_number_type")
    @JsonProperty("idcard_number_type")
    private int idcardNumberType;

    @Data
    public static class CardQuality {
        @JSONField(name = "IsClear")
        @JsonProperty("IsClear")
        private String isClear;

        @JSONField(name = "IsClear_propobility")
        @JsonProperty("IsClear_propobility")
        private String isClearPropobility;

        @JSONField(name = "IsComplete")
        @JsonProperty("IsComplete")
        private String isComplete;

        @JSONField(name = "IsComplete_propobility")
        @JsonProperty("IsComplete_propobility")
        private String isCompletePropobility;

        @JSONField(name = "IsNoCover")
        @JsonProperty("IsNoCover")
        private String isNoCover;

        @JSONField(name = "IsNoCover_propobility")
        @JsonProperty("IsNoCover_propobility")
        private String isNoCoverPropobility;
    }

    @Data
    public static class Location {
        @JSONField(name = "left")
        @JsonProperty("left")
        private int left;

        @JSONField(name = "top")
        @JsonProperty("top")
        private int top;

        @JSONField(name = "width")
        @JsonProperty("width")
        private int width;

        @JSONField(name = "height")
        @JsonProperty("height")
        private int height;
    }
}
