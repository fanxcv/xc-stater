package `fun`.fan.xc.plugin.token_manager

interface TokenManager {
    /**
     * token key
     * 用于唯一标识
     */
    fun key(): String

    /**
     * 获取Token
     */
    fun token(): String

    /**
     * 获取Token到期时间
     */
    fun expires(): Long

    /**
     * 初始化Token
     */
    fun init()

    /**
     * 刷新Token
     */
    fun refresh()

    open class BaseTokenEntity {
        /**
         * token
         */
        var token: String? = null

        /**
         * 到期时间，单位ms
         */
        var expires: Long = 0

        /**
         * 下次刷新时间，单位ms
         */
        var refresh: Long = 0
    }
}
