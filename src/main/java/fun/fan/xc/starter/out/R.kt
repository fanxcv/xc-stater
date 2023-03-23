package `fun`.fan.xc.starter.out

import `fun`.fan.xc.starter.enums.ReturnCode
import org.springframework.http.HttpStatus

class R<D> private constructor() : HashMap<String, Any?>() {
    companion object {
        @Transient
        private val message = "msg"

        @Transient
        private val body = "body"

        @Transient
        private val code = "code"

        @JvmStatic
        fun <D> build() = success<D>()

        @JvmStatic
        fun <D> success(data: D?): R<D> = R<D>().setReturn(ReturnCode.SUCCESS).setBody(data)

        @JvmStatic
        fun <D> success(): R<D> = R<D>().setReturn(ReturnCode.SUCCESS)

        @JvmStatic
        fun <D> fail(rc: ReturnCode): R<D> = R<D>().setReturn(rc)

        @JvmStatic
        fun <D> fail(code: Int, msg: String?): R<D> =
            R<D>().setCode(code).setMsg(msg).setStatus(HttpStatus.INTERNAL_SERVER_ERROR)

        @JvmStatic
        fun <D> fail(msg: String?): R<D> =
            R<D>().setReturn(ReturnCode.FAIL).setMsg(msg)

        @JvmStatic
        fun <D> fail(): R<D> = R<D>().setReturn(ReturnCode.FAIL)
    }

    @Transient
    private var status: HttpStatus = HttpStatus.OK

    init {
        this[code] = ReturnCode.SUCCESS.code()
        this[message] = ReturnCode.SUCCESS.message()
    }

    fun getStatus(): HttpStatus = status

    fun setReturn(rc: ReturnCode): R<D> {
        this.status = rc.status()
        this[code] = rc.code()
        this[message] = rc.message()
        return this
    }

    fun setCode(code: Int): R<D> {
        this[R.code] = code
        return this
    }

    fun setMsg(msg: String?): R<D> {
        this[message] = msg
        return this
    }

    fun setBody(body: D?): R<D> {
        this[R.body] = body
        return this
    }

    fun setStatus(status: HttpStatus): R<D> {
        this.status = status
        return this
    }

    fun set(key: String, value: Any?): R<D> {
        this[key] = value
        return this
    }
}