package `fun`.fan.xc.plugin.meituan

import `fun`.fan.xc.starter.utils.EncryptUtils
import java.util.*

object MtUtils {
    const val AUTH_URI = "https://open-erp.meituan.com/general/auth"
    const val TOKEN_URI = "https://api-open-cater.meituan.com/oauth/token"
    const val REFRESH_URI = "https://api-open-cater.meituan.com/oauth/refresh"

    /**
     * 美团签名算法
     */
    fun sign(params: Map<String, Any?>, key: String): String {
        val sb = StringBuilder()
        params.filter { it.value != null && it.value.toString() != "" && it.key != "sign" }
            .map { it.key to it.value }
            .sortedBy { it.first }
            .forEach { sb.append("${it.first}${it.second}") }
        sb.insert(0, key)
        return EncryptUtils.sha1(sb.toString()).lowercase(Locale.getDefault())
    }
}
