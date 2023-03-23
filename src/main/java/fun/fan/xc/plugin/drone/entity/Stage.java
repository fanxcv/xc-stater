package fun.fan.xc.plugin.drone.entity;

import lombok.Data;

import java.util.List;

/**
 * @author fan
 */
@Data
public class Stage {
    private long id;
    private long repo_id;
    private long build_id;
    private long number;
    private String name;
    private String kind;
    private String type;
    private String status;
    private boolean errignore;
    private long exit_code;
    private String machine;
    private String os;
    private String arch;
    private long started;
    private long stopped;
    private long created;
    private long updated;
    private long version;
    private boolean on_success;
    private boolean on_failure;
    private List<Step> steps;
}
