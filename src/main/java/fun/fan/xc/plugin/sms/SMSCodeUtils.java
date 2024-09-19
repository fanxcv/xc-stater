package fun.fan.xc.plugin.sms;

import cn.hutool.core.util.StrUtil;
import fun.fan.xc.plugin.redis.Redis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SMSCodeUtils {
    private final Redis redis;

    /**
     * 生成短信验证码
     * 可以指定同一手机号的发送频率，默认60秒一次
     * 可以指定短信验证码的有效时间， 默认10分钟
     * 还需要限制一个手机号在指定时间内最多发送的次数，默认一小时5次
     *
     * @param phoneNumber  手机号
     * @param business     业务类型
     * @param sendSettings 发送设置
     * @return 短信验证码
     */
    public String generateSMSCode(String phoneNumber, String business, SendSettings sendSettings) {
        if (sendSettings == null) {
            sendSettings = new SendSettings();
        }
        if (StrUtil.isBlank(business)) {
            business = "default";
        }
        // 先校验这个手机号是否在频率限制内
        String frequencyKey = "sms:" + phoneNumber + ":" + business + ":frequency";
        if (redis.exists(frequencyKey)) {
            throw new RuntimeException("发送频率过高，请稍后再试");
        }
        // 再校验这个手机号是否在发送次数限制内
        String countKey = "sms:" + phoneNumber + ":" + business + ":count";
        long count = redis.incr(countKey);
        if (count > sendSettings.getMaxSendCount()) {
            throw new RuntimeException("发送次数过多，请稍后再试");
        }
        if (count == 1) {
            // 如果count为1，表示是第一次发送，设置过期时间
            redis.expire(countKey, 1, TimeUnit.HOURS);
        }

        // 生成6位随机数作为短信验证码
        String code = String.format("%06d", new Random().nextInt(1000000));
        String codeKey = "sms:" + phoneNumber + ":" + business + ":code";

        // 将短信验证码存入Redis，设置过期时间
        redis.setEx(codeKey, code, sendSettings.getValidityTime(), TimeUnit.MINUTES);
        // 保存频率限制
        redis.setEx(frequencyKey, "1", sendSettings.getFrequency(), TimeUnit.SECONDS);

        return code;
    }

    public String generateSMSCode(String phoneNumber) {
        return generateSMSCode(phoneNumber, "default", null);
    }

    /**
     * 校验短信验证码
     *
     * @param phoneNumber 手机号
     * @param business    业务类型
     * @param code        短信验证码
     * @return 是否校验通过
     */
    public boolean verifySMSCode(String phoneNumber, String business, String code) {
        if (StrUtil.isBlank(business)) {
            business = "default";
        }
        String codeKey = "sms:" + phoneNumber + ":" + business + ":code";
        String codeInRedis = redis.get(codeKey);
        if (StrUtil.isBlank(codeInRedis)) {
            return false;
        }
        if (codeInRedis.equals(code)) {
            String frequencyKey = "sms:" + phoneNumber + ":" + business + ":frequency";
            // 校验通过，删除短信验证码
            redis.del(codeKey, frequencyKey);
            return true;
        }
        return false;
    }

    public boolean verifySMSCode(String phoneNumber, String code) {
        return verifySMSCode(phoneNumber, "default", code);
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendSettings {
        /**
         * 同一手机号的发送频率，默认60秒一次
         */
        private int frequency = 60;
        /**
         * 短信验证码的有效时间， 默认10分钟
         */
        private int validityTime = 10;
        /**
         * 一个手机号在指定时间内最多发送的次数，默认一小时5次
         */
        private int maxSendCount = 5;
    }
}
