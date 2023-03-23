package fun.fan.xc.plugin.drone;

import fun.fan.xc.plugin.drone.service.DroneBuildService;
import fun.fan.xc.plugin.drone.service.DroneRepoService;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        DroneRepoService.class,
        DroneBuildService.class
})
public @interface EnableDroneApi {
}
