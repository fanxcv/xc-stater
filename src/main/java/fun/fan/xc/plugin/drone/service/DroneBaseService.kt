package `fun`.fan.xc.plugin.drone.service

import `fun`.fan.xc.starter.XcConfiguration
import `fun`.fan.xc.starter.utils.NetUtils.build
import java.lang.reflect.Type

/**
 * @author fan
 */
open class DroneBaseService(private val config: XcConfiguration) {
    protected operator fun <T> get(uri: String, type: Type): T {
        return get(uri, null, type)
    }

    protected operator fun <T> get(uri: String, parameters: Map<String, Any?>?, type: Type): T {
        return build(config.drone.host + uri)
            .authorization(config.drone.token)
            .addParams(parameters)
            .doGet(type)
    }

    protected fun <T> post(uri: String, parameters: Map<String, Any?>?, type: Type): T {
        return build(config.drone.host + uri)
            .authorization(config.drone.token)
            .addParams(parameters)
            .doPost(type)
    }
}
