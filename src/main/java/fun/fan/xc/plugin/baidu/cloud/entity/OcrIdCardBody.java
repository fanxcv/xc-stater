package fun.fan.xc.plugin.baidu.cloud.entity;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.net.URLEncoder;
import java.util.Map;

/**
 * { @link "<a href="https://cloud.baidu.com/doc/OCR/s/rk3h7xzck">文档地址</a>" }
 */
@Data
@Accessors(chain = true)
public class OcrIdCardBody {
    /**
     * 必填
     * 和url二选一
     * 图像数据，base64编码后进行urlencode，需去掉编码头（data:image/jpeg;base64, ）
     * 要求base64编码和urlencode后大小不超过8M，最短边至少15px，最长边最大8192px，支持jpg/jpeg/png/bmp格式
     */
    private String image;
    /**
     * 必填
     * 和image二选一
     * 图片完整URL，URL长度不超过1024字节，URL对应的图片base64编码后大小不超过8M，最短边至少15px，最长边最大8192px，支持jpg/jpeg/png/bmp格式，当image字段存在时url字段失效
     * 请注意关闭URL防盗链
     */
    private String url;

    /**
     * 必填
     * -front：身份证含照片的一面
     * -back：身份证带国徽的一面
     * 自动检测身份证正反面，如果传参指定方向与图片相反，支持正常识别，返回参数image_status字段为"reversed_side"
     */
    @JSONField(name = "id_card_side")
    @JsonProperty("id_card_side")
    private String idCardSide;

    /**
     * 是否检测上传的身份证被PS，默认不检测。可选值：
     * -true：检测
     * - false：不检测
     */
    @JSONField(name = "detect_ps")
    @JsonProperty("detect_ps")
    private String detectPs;

    /**
     * 是否开启身份证风险类型（身份证复印件、临时身份证、身份证翻拍、修改过的身份证）检测功能，默认不开启，即：false。
     * - true：开启，请查看返回参数risk_type；
     * - false：不开启
     */
    @JSONField(name = "detect_risk")
    @JsonProperty("detect_risk")
    private String detectRisk;

    /**
     * 是否开启身份证质量类型（清晰模糊、边框/四角不完整、头像或关键字段被遮挡/马赛克）检测功能，默认不开启，即：false。
     * - true：开启，请查看返回参数card_quality；
     * - false：不开启
     */
    @JSONField(name = "detect_quality")
    @JsonProperty("detect_quality")
    private String detectQuality;

    /**
     * 是否检测头像内容，默认不检测。可选值：true-检测头像并返回头像的 base64 编码及位置信息
     */
    @JSONField(name = "detect_photo")
    @JsonProperty("detect_photo")
    private String detectPhoto;

    /**
     * 是否检测身份证进行裁剪，默认不检测。可选值：true-检测身份证并返回证照的 base64 编码及位置信息
     */
    @JSONField(name = "detect_card")
    @JsonProperty("detect_card")
    private String detectCard;

    /**
     * 是否检测上传的身份证图片方向，默认不检测。可选值：
     * -true：检测
     * - false：不检测
     */
    @JSONField(name = "detect_direction")
    @JsonProperty("detect_direction")
    private String detectDirection;


    @SneakyThrows
    public Map<String, Object> toRequestBody() {
        Map<String, Object> map = Maps.newHashMap();

        map.put("id_card_side", this.idCardSide);

        if (StrUtil.isNotBlank(this.image)) {
            map.put("image", URLEncoder.encode(this.image, "UTF-8"));
        }
        if (StrUtil.isNotBlank(this.url)) {
            map.put("url", this.url);
        }

        if (StrUtil.isNotBlank(this.detectPs)) {
            map.put("detect_ps", this.detectPs);
        }
        if (StrUtil.isNotBlank(this.detectRisk)) {
            map.put("detect_risk", this.detectRisk);
        }
        if (StrUtil.isNotBlank(this.detectQuality)) {
            map.put("detect_quality", this.detectQuality);
        }
        if (StrUtil.isNotBlank(this.detectPhoto)) {
            map.put("detect_photo", this.detectPhoto);
        }
        if (StrUtil.isNotBlank(this.detectCard)) {
            map.put("detect_card", this.detectCard);
        }
        if (StrUtil.isNotBlank(this.detectDirection)) {
            map.put("detect_direction", this.detectDirection);
        }

        return map;
    }
}
