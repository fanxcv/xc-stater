package fun.fan.xc.plugin.drone.entity;

import lombok.Data;

/**
 * @author fan
 */
@Data
public class BuildCreate {
    private String namespace;
    private String name;
    private String branch;
    /**
     * 多个模块逗号隔开
     */
    private String modules;
    private String env;

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch;
    }

    public String getModules() {
        return modules;
    }

    public String getEnv() {
        return env;
    }
}
