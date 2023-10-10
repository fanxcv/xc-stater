package `fun`.fan.xc.starter.utils

/**
 * 集合工具类
 */
object XcCollections {
    @JvmStatic
    fun <K, T> toMap(collection: Collection<T>, keyMapper: (T) -> K): Map<K, T> {
        val map: MutableMap<K, T> = HashMap()
        collection.forEach {
            map[keyMapper(it)] = it
        }
        return map
    }
}
