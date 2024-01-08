package `fun`.fan.xc.starter.wrapper

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * 可重复读取的HttpServletRequest实现
 *
 * @author fan
 */
class BufferedServletRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    private val body: ByteArray

    init {
        body = request.inputStream.use { it.readBytes() }
    }

    override fun getReader(): BufferedReader {
        return BufferedReader(InputStreamReader(inputStream))
    }

    override fun getInputStream(): ServletInputStream {
        val inputStream = ByteArrayInputStream(body)
        return object : ServletInputStream() {
            override fun isFinished(): Boolean = false

            override fun isReady(): Boolean = true

            override fun setReadListener(listener: ReadListener) {}

            override fun read(): Int = inputStream.read()
        }
    }
}
