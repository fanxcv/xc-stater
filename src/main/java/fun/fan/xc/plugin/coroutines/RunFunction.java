package fun.fan.xc.plugin.coroutines;

/**
 * @author fan
 */
@FunctionalInterface
public interface RunFunction {
    /**
     * 待执行的方法
     *
     * @throws Throwable 异常
     */
    void run() throws Throwable;
}
