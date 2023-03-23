package fun.fan.xc.plugin.drone.entity;

import lombok.Data;

import java.util.List;

/**
 * @author fan
 */
@Data
public class Build {
    private long id;
    private long repo_id;
    private long number;
    private String status;
    private String event;
    private String action;
    private String link;
    private String message;
    private String before;
    private String after;
    private String ref;
    private String source_repo;
    private String source;
    private String target;
    private String author_login;
    private String author_name;
    private String author_email;
    private String author_avatar;
    private String sender;
    private long started;
    private long finished;
    private long created;
    private long updated;
    private long version;
    private List<Stage> stages;
}