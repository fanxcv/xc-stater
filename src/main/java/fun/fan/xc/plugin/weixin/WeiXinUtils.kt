package `fun`.fan.xc.plugin.weixin

import cn.hutool.core.codec.Base64
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.plugin.weixin.entity.PayBase
import `fun`.fan.xc.plugin.weixin.entity.PayRefundNotifyResp
import `fun`.fan.xc.starter.utils.BeanUtils
import `fun`.fan.xc.starter.utils.EncryptUtils
import `fun`.fan.xc.starter.utils.NetUtils
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object WeiXinUtils {
    /**
     * 微信签名
     */
    fun sign(params: Map<String, Any?>, key: String): String {
        val sb = StringBuilder()
        params.filter { it.value != null && it.value.toString() != "" }
            .map { it.key to it.value }
            .sortedBy { it.first }
            .forEach { sb.append("${it.first}=${it.second}&") }
        sb.append("key=$key")
        return EncryptUtils.md5(sb.toString()).uppercase(Locale.getDefault())
    }

    fun sign(params: PayBase, key: String) {
        if (params.sign.isNullOrBlank()) {
            val map = BeanUtils.beanToMap(params) {
                val property = it.getAnnotation(JsonProperty::class.java)
                property?.value ?: it.name
            }
            val sign = sign(map, key)
            params.sign = sign
        }
    }

    fun decodeRefundInfo(reqInfo: String, key: String): PayRefundNotifyResp.RefundInfo {
        val bytes = Base64.decode(reqInfo)
        val keySpec = SecretKeySpec(EncryptUtils.md5(key).lowercase(Locale.getDefault()).toByteArray(), "AES")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val buffer = cipher.doFinal(bytes)
        return NetUtils.XMLMapper.readValue(buffer, PayRefundNotifyResp.RefundInfo::class.java)
    }
}