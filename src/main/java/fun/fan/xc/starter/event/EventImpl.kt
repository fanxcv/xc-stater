package `fun`.fan.xc.starter.event

import cn.hutool.core.convert.Convert
import com.google.common.collect.Maps
import `fun`.fan.xc.starter.utils.Conversion
import java.lang.reflect.Type

/**
 * 数据存储结构
 */
class EventImpl private constructor() : EventInner, HashMap<String, Any?>() {
    companion object {
        @Transient
        private val threadLocal: ThreadLocal<EventInner> = ThreadLocal()

        @Transient
        private val message = "msg"

        @Transient
        private val body = "body"

        @Transient
        private val code = "code"

        @Transient
        private val path = "path"

        @Transient
        private val time = "time"

        @JvmStatic
        fun getEvent(): EventInner = threadLocal.get() ?: instance()

        @JvmStatic
        fun removeEvent() = threadLocal.remove()

        fun instance(): EventInner {
            var event: EventInner? = threadLocal.get()
            return if (event == null) {
                event = EventImpl()
                threadLocal.set(event)
                event
            } else {
                event.init()
            }
        }
    }

    /**
     * 请求入参
     */
    @Transient
    private val params: MutableMap<String, Any?> = HashMap()

    /**
     * 请求token
     */
    @Transient
    private var token: String? = null

    override fun init(): EventInner {
        this.params.clear()
        this.clear()
        return this
    }

    override fun putParamAll(m: Map<out String, *>?) {
        if (m != null) {
            params.putAll(m)
        }
    }

    override fun putParam(key: String, value: Any?): Any? = params.put(key, value)

    override fun setToken(token: String?) {
        this.token = token
    }

    override fun getToken(): String? = this.token

    override fun getParam(key: String): Any? = params[key]

    override fun <T> getParam(key: String, type: Type): T? = Convert.convert(type, params[key])

    override fun getParamMap(): MutableMap<String, Any?> = Maps.newHashMap(params)

    override fun <T> getBean(clazz: Class<T>): T? = Conversion.binder.convertIfNecessary(params, clazz)
}
