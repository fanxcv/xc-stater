package fun.fan.xc.plugin.drone.entity;

import lombok.Data;

/**
 * @author fan
 */
@Data
public class Step {
    private long id;
    private long step_id;
    private long number;
    private String name;
    private String status;
    private long exit_code;
    private long started;
    private long stopped;
    private long version;
}
