package fun.fan.xc.plugin.ca.mcs;

import cn.com.mcsca.pki.core.util.SignatureUtil;
import fun.fan.xc.plugin.ca.mcs.entity.ReqHead;
import fun.fan.xc.plugin.ca.mcs.entity.Request;
import fun.fan.xc.starter.exception.XcToolsException;

import java.util.HashMap;
import java.util.Map;

public class McsUtils {
    private static McsConfig config;

    public McsUtils(McsConfig config) {
        McsUtils.config = config;
        // 校验依赖是否被加载
        try {
            Class.forName("cn.com.mcsca.pki.core.util.SignatureUtil");
        } catch (ClassNotFoundException e) {
            throw new XcToolsException("未加载MCS的依赖, 请引入pki-core包");
        }
    }

    public static <T> String doSign(Request<T> request) {
        Map<String, Object> signMap = new HashMap<>(4);
        // 进行签名
        signMap.put("reqHead", request.getReqHead());
        signMap.put("reqBody", request.getReqBody());
        String json = SignatureUtil.getSortJsonStr(signMap);
        return SignatureUtil.doSign(config.getPrivateKey(), json);
    }

    public static void buildHead(ReqHead head) {
        head.setCustomerId(config.getCustomerId())
                .setVersion(config.getVersion())
                .setAppId(config.getAppId());
    }
}
