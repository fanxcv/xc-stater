package `fun`.fan.xc.plugin.weixin

import `fun`.fan.xc.starter.utils.EncryptUtils
import java.lang.StringBuilder
import java.util.*

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
}