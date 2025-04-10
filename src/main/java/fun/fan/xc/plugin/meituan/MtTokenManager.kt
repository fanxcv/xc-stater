package `fun`.fan.xc.plugin.meituan

interface MtTokenManager {
    fun initToken(code: String)

    fun refreshToken()

    fun getToken(): String
}
