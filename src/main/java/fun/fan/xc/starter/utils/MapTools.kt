package `fun`.fan.xc.starter.utils

import cn.hutool.core.lang.Assert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

object MapTools {
    private val log: Logger = LoggerFactory.getLogger(MapTools::class.java)

    @JvmStatic
    fun <K, V> getOrInit(map: MutableMap<K, V>?, key: K, supplier: Supplier<V>): V? {
        if (map.isNullOrEmpty()) {
            return null
        }
        return map[key] ?: supplier.get().also {
            Assert.notNull(it, "初始化数据不能为null")
            map[key] = it
        }
    }
}
