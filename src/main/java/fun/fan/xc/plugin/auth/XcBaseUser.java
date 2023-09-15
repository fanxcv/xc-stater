package fun.fan.xc.plugin.auth;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户基础类
 *
 * @author fan
 */
@Data
@Accessors(chain = true)
@Schema(description = "基础用户信息")
public class XcBaseUser {
    @TableField
    @Schema(description = "用户账户")
    private String account;

    @TableField
    @Schema(description = "用户名")
    private String name;

    @TableField(exist = false)
    @Schema(description = "登录返回Token")
    private String token;

    @Hidden
    @JsonIgnore
    @TableField(exist = false)
    @JSONField(serialize = false)
    private String client = AuthConstant.DEFAULT_CLIENT;
}
