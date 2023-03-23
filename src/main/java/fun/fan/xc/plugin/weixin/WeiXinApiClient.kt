package `fun`.fan.xc.plugin.weixin

interface WeiXinApiClient {
    /**
     * 获取accessToken
     */
    fun accessToken(): String

    /**
     * 获取jsTicket
     */
    fun jsTicket(): String = ""
}