package `fun`.fan.xc.plugin.weixin.token

interface WeiXinTokenManager {
    /**
     * 用于唯一标识, 用于区分多个Token管理任务, 打印日志的
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
     * 初始化方法, 会在项目启动时调用
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
