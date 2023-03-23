package `fun`.fan.xc.plugin.weixin

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "xc.weixin.client", value = ["enable"], havingValue = "true", matchIfMissing = false)
class WeixinClientEnable {
}