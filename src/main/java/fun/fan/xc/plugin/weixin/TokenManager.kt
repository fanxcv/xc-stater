package `fun`.fan.xc.plugin.weixin

interface TokenManager {
    /**
     * 获取Token
     */
    fun token(): String

    /**
     * 获取Token到期时间
     */
    fun expires(): Long

    /**
     * 强制触发Token刷新
     */
    fun initToken()

    /**
     * 初始化锁
     */
    fun initLock()

    /**
     * token name
     */
    fun name(): String
}