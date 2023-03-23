package fun.fan.xc.plugin.drone.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author fan
 */
@Data
public class Repo {
    private long id;
    private String uid;
    @JsonProperty("user_id")
    private long userId;
    private String namespace;
    private String name;
    private String slug;
    private String scm;
    @JsonProperty("git_http_url")
    private String gitHttpUrl;
    @JsonProperty("git_ssh_url")
    private String gitSshUrl;
    private String link;
    @JsonProperty("default_branch")
    private String defaultBranch;
    @JsonProperty("private")
    private boolean isPrivate;
    private String visibility;
    private boolean active;
    @JsonProperty("config_path")
    private String configPath;
    private boolean trusted;
    @JsonProperty("protected")
    private boolean isProtected;
    @JsonProperty("ignore_forks")
    private boolean ignoreForks;
    @JsonProperty("ignore_pull_requests")
    private boolean ignorePullRequests;
    private long timeout;
    private long counter;
    private long synced;
    private long created;
    private long updated;
    private long version;
    private Permissions permissions;

    @Data
    static class Permissions {
        private Boolean read;
        private Boolean write;
        private Boolean admin;
    }
}
