package fun.fan.xc.plugin.ca.mcs.entity;

import fun.fan.xc.plugin.ca.mcs.McsUtils;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Accessors(chain = true)
public class Request<T> {
    /**
     * 请求头
     */
    private ReqHead reqHead;
    /**
     * 请求体
     */
    private T reqBody;
    /**
     * 签名值
     */
    private String sign;

    private Request() {

    }

    // 创建者模式实现
    @Data
    @Accessors(chain = true)
    public static class Builder<T> {
        /**
         * 业务流水号
         */
        private String serialNo;

        /**
         * 透传参数
         */
        private String attach;

        private T reqBody;

        public Request<T> build() {
            Request<T> request = new Request<>();
            ReqHead head = new ReqHead()
                    .setReqTime(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()))
                    .setSerialNo(serialNo)
                    .setAttach(attach);
            // 填充配置项
            McsUtils.buildHead(head);
            request.setReqHead(head).setReqBody(reqBody);
            request.setSign(McsUtils.doSign(request));
            return request;
        }
    }
}
