package fun.fan.xc.plugin.proxy;

import fun.fan.xc.plugin.proxy.client.NettyConnectionPool;
import fun.fan.xc.plugin.proxy.client.ProxyClient;
import fun.fan.xc.plugin.proxy.handler.ProxyRequestHandler;
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
        NettyConnectionPool.class,
        ProxyRequestHandler.class,
        XcProxyAutoConfiguration.class
})
public @interface EnableXcProxy {
}
