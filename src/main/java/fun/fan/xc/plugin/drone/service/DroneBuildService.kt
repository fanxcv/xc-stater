package `fun`.fan.xc.plugin.drone.service

import `fun`.fan.xc.plugin.drone.entity.Build
import `fun`.fan.xc.plugin.drone.entity.BuildCreate
import `fun`.fan.xc.starter.XcConfiguration
import org.springframework.stereotype.Service

/**
 * @author fan
 */
@Service
class DroneBuildService(config: XcConfiguration) : DroneBaseService(config) {
    fun create(create: BuildCreate): Build {
        return post(
            "/api/repos/${create.namespace}/${create.name}/builds?branch=${create.branch}&ENV=${create.env}&MODULE=${create.modules}",
            null,
            Build::class.java
        )
    }
}