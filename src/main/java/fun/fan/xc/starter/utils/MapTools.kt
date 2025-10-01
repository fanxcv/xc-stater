package `fun`.fan.xc.starter.utils

import cn.hutool.core.lang.Assert
import java.util.function.Supplier

object MapTools {

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
