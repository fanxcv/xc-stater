package `fun`.fan.xc.starter.event

interface EventInner : Event {
    /**
     * 存储所有的请求参数
     * @param m 待存储的键值对
     */
    fun putParamAll(m: Map<out String, *>?)

    /**
     * 向参数表中添加单个键值
     * @param key key
     * @param value value
     * @return value
     */
    fun putParam(key: String, value: Any?): Any?

    /**
     * 添加token
     * @param token token
     */
    fun setToken(token: String?)

    /**
     * 初始化Event对象，用于清空旧数据
     * @return Event对象
     */
    fun init(): EventInner
}