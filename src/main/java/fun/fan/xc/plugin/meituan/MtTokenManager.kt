package `fun`.fan.xc.plugin.meituan

import com.alibaba.fastjson2.JSONObject

interface MtTokenManager {
    fun initToken(code: String, key: String?): JSONObject

    fun refreshToken(key: String)

    fun getToken(key: String): String
}
