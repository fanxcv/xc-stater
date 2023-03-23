package `fun`.fan.xc.plugin.drone.service

import com.fasterxml.jackson.core.type.TypeReference
import `fun`.fan.xc.plugin.drone.entity.Repo
import `fun`.fan.xc.starter.XcConfiguration
import org.springframework.stereotype.Service

/**
 * @author fan
 */
@Service
class DroneRepoService(config: XcConfiguration) : DroneBaseService(config) {
    fun list(): List<Repo> {
        return get("/api/user/repos?latest=true", object : TypeReference<List<Repo>>() {}.type)
    }

    fun info(namespace: String, name: String): Repo {
        return get("/api/repos/$namespace/$name", Repo::class.java)
    }
}