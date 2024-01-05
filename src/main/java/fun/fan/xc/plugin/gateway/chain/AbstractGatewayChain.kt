package `fun`.fan.xc.plugin.gateway.chain

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Slf4j
abstract class AbstractGatewayChain<V> {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    private var next: AbstractGatewayChain<V>? = null

    companion object {
        @JvmStatic
        fun <V> builder(): Builder<V> {
            return Builder()
        }
    }

    class Builder<V> {
        private var head: AbstractGatewayChain<V>? = null
        private var tail: AbstractGatewayChain<V>? = null

        fun addChain(chain: AbstractGatewayChain<V>): Builder<V> {
            if (head == null) {
                head = chain
                tail = chain
            } else {
                tail?.next = chain
                tail = chain
            }
            return this
        }

        fun build(): AbstractGatewayChain<V>? {
            return head
        }
    }

    /**
     * 触发方法
     * 如果doExec执行无明确结果,即Null,继续执行后续流程,否则明确返回true,false
     */
    fun exec(v: V): Boolean? {
        val res = doExec(v)
        return res ?: next?.exec(v)
    }

    /**
     * 逻辑执行
     * @return 校验结果,无明确结果返回Null, T为传入到下个doExec的参数,可为null
     */
    abstract fun doExec(v: V): Boolean?

    fun chain(): String {
        val sb = StringBuilder()
        var next: AbstractGatewayChain<V>? = this
        while (next != null) {
            sb.append(next.javaClass.simpleName).append(" -> ")
            next = next.next
        }
        val len = sb.length
        return sb.delete(len - 4, len).toString();
    }

}
