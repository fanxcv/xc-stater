package fun.fan.xc.plugin.weixin;

import fun.fan.xc.plugin.weixin.official.*;
import fun.fan.xc.plugin.weixin.program.ProgramAccessTokenManager;
import fun.fan.xc.plugin.weixin.program.ProgramWeiXinApi;
import fun.fan.xc.plugin.weixin.program.ProgramWeiXinApiClient;
import fun.fan.xc.plugin.weixin.program.ProgramWeiXinApiController;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author fan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({
        WeixinClientEnable.class,
        WeixinServerEnable.class,
        WeiXinInterceptor.class,
        WeiXinApiController.class,
        OfficialWeiXinApi.class,
        OfficialWeiXinApiClient.class,
        OfficialAccessTokenManager.class,
        OfficialJsApiTicketManager.class,
        OfficialWeiXinApiController.class,
        ProgramWeiXinApiController.class,
        ProgramAccessTokenManager.class,
        ProgramWeiXinApiClient.class,
        ProgramWeiXinApi.class
})
@EnableConfigurationProperties(WeiXinConfig.class)
public @interface EnableWeiXinApi {
}
