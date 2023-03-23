package `fun`.fan.xc.plugin.coroutines

import kotlinx.coroutines.*

object KotlinCoroutines {
    private suspend fun <R> scope(block: suspend CoroutineScope.() -> R) = coroutineScope {
        block()
    }

    /**
     * 替换多线程执行异步方法
     */
    @JvmStatic
    fun run(run: RunFunction) = runBlocking {
        scope {
            launch {
                run.run()
            }
        }
    }

    /**
     * 用于在协程中等待
     */
    @JvmStatic
    suspend fun sleep(time: Long) = delay(time)
}