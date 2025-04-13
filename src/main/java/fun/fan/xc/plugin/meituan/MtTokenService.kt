package `fun`.fan.xc.plugin.meituan

import com.alibaba.fastjson2.JSONObject
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class MtTokenService(
    private val config: MtConfig,
    private val tokenManager: MtTokenManager
) {
    /**
     * 生成授权Uri
     * 将授权Uri直接返回给前端打开即可
     * @param state ISV自定义字段，授权完成时，此值会回值给ISV。主要作用是防止跨站请求伪造（CSRF）攻击；辅助能力用于保持请求和回调之间的状态，可用此参数映射ISV自己的门店id或唯一标识
     * @param scope 授权权限范围。
     *              不使用参数：展示开发者全部可用权限，由授权人决定授权权限范围；
     *              使用参数且传值：展示参数对应的开发者可用权限，授权人不可编辑；
     *              使用参数未传值：等同于不使用参数；
     *              注：授权为更新机制，多次授权时，scope参数值应为全部所需，多个scope之间通过逗号","分割，不可只传增量。
     *              参考：https://developer.meituan.com/docs/biz/biz_2023243_3ad0b0e7-01d4-40a8-8a3f-2e6f6ae32f23
     */
    fun generateAuthUri(state: String? = "", scope: String? = ""): String {
        mutableMapOf(
            "developerId" to config.developerId,
            "businessId" to config.businessId,
            "timestamp" to System.currentTimeMillis() / 1000,
            "charset" to "UTF-8",
            "qrMode" to "1",
            "state" to (state ?: ""),
            "scope" to (scope ?: ""),
        ).let { params ->
            params["sign"] = MtUtils.sign(params, config.signKey)
            return MtUtils.AUTH_URI + "?" + params.entries.joinToString("&") { "${it.key}=${it.value}" }
        }
    }

    /**
     * 授权回调
     * @param code 授权码
     * @param key 用于区分不同的商户授权码的Key, 不传或默认获取opBizCode作为Key
     */
    fun authCallback(code: String, key: String?): JSONObject = tokenManager.initToken(code, key)

    fun token(key: String) = tokenManager.getToken(key)
}
