package `fun`.fan.xc.starter.event

import java.lang.reflect.Type

interface Event {
    /**
     * 获取token
     * @return token
     */
    fun getToken(): String?

    /**
     * 返回一个参数
     * @param key 参数名
     * @return 参数值
     */
    fun getParam(key: String): Any?

    /**
     * 返回一个参数
     * @param key   参数名
     * @param clazz 参数类型
     * @return 参数值
     */
    fun <T> getParam(key: String, type: Type): T?

    /**
     * 返回所有参数所在的集合，镜像集合
     * @return 参数集合
     */
    fun getParamMap(): MutableMap<String, Any?>

    /**
     * 把参数封装成bean后返回
     * @param type 待封装的Bean Class
     * @return Bean对象
     */
    fun <T> getBean(clazz: Class<T>): T?
}