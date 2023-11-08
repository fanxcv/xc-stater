package `fun`.fan.xc.plugin.weixin

import `fun`.fan.xc.starter.out.R
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.stream.Collectors

@RestController
@ConditionalOnBean(WeixinServerEnable::class)
@RequestMapping("\${xc.weixin.server.base-path:${WeiXinDict.WX_BASE_PATH}}")
class WeiXinApiController(
    private val config: WeiXinConfig
) : InitializingBean, ApplicationContextAware {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var tokenMangers: Collection<TokenManager>

    /**
     * 获取所有token
     * @return accessToken和jsTicket
     */
    @GetMapping("token")
    fun getToken(): R<Map<String, Any>> {
        return R.success(
            tokenMangers.stream().collect(
                Collectors.toMap({ it.name() }, { it.token() })
            )
        )
    }

    /**
     * 初始化刷新锁, 避免死锁必须重启服务器
     */
    @PutMapping("initLock")
    fun initLock() {
        tokenMangers.forEach { it.initLock() }
    }

    /**
     * 强制触发Token刷新
     */
    @PutMapping("initToken")
    fun initToken() {
        tokenMangers.forEach { it.initToken() }
    }

    override fun afterPropertiesSet() {
        val path =
            if (StringUtils.isNotBlank(config.server.basePath)) config.server.basePath else WeiXinDict.WX_BASE_PATH
        log.info("===> weixin: Public api is running, entrypoint: $path/{api}")
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.tokenMangers = applicationContext.getBeansOfType(TokenManager::class.java).values
    }
}
