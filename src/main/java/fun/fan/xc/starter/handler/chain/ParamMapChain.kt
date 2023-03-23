package `fun`.fan.xc.starter.handler.chain

import `fun`.fan.xc.starter.annotation.VerifyParam
import `fun`.fan.xc.starter.interfaces.ParamMap
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * 参数预处理
 */
@Component
class ParamMapChain(private val beanFactory: BeanFactory) : AbstractVerifyChain() {
    companion object {
        private val beanCache: MutableMap<KClass<out ParamMap>, ParamMap> = HashMap()
    }

    override fun doExec(key: String, value: Any?, verify: VerifyParam): Pair<Boolean, Any?> {
        var v = value
        // 参数预处理
        val maps: Array<KClass<out ParamMap>> = verify.map
        for (map in maps) {
            val clazz: ParamMap = beanCache[map] ?: fun(): ParamMap {
                val bean = beanFactory.getBean(map.java)
                beanCache[map] = bean
                return bean
            }.invoke()
            v = clazz.map(v)
        }
        return Pair(true, v)
    }
}