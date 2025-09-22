package fun.fan.xc.plugin.proxy;

import fun.fan.xc.plugin.proxy.client.HostPortChannelPool;
import fun.fan.xc.plugin.proxy.client.ProxyClient;
import fun.fan.xc.plugin.proxy.config.ProxyConfigurationManager;
import fun.fan.xc.plugin.proxy.orchestrator.ProxyOrchestrator;
import fun.fan.xc.plugin.proxy.transformer.HttpResponseTransformer;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用Xc代理功能
 *
 * @author fan
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        ProxyClient.class,
        ProxyOrchestrator.class,
        HostPortChannelPool.class,
        HttpResponseTransformer.class,
        XcProxyAutoConfiguration.class,
        ProxyConfigurationManager.class,
})
public @interface EnableXcProxy {
}
