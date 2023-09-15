package `fun`.fan.xc.starter.utils

import com.alibaba.fastjson2.JSON
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import `fun`.fan.xc.starter.exception.XcToolsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.io.*
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 基于HttpConnection的网络请求实现
 */
object NetUtils {
    private val log: Logger = LoggerFactory.getLogger(NetUtils::class.java)
    private const val PREFIX = "--"
    private const val LINE_END = "\r\n"

    val XMLMapper: XmlMapper by lazy {
        XmlMapper()
    }

    data class Upload(val fileName: String, val `is`: InputStream) {
        var type: String = MediaType.APPLICATION_OCTET_STREAM_VALUE
    }

    @JvmStatic
    fun build(): Builder = Builder()

    @JvmStatic
    fun build(url: String): Builder = Builder(url)

    class Builder {
        private var url: String = ""
        private var body: Any? = null
        private var readTimeout: Int = 30
        private var connectTimeout: Int = 10
        private var contentType: MediaType = MediaType.APPLICATION_JSON
        private val headers: MutableMap<String, String> = Maps.newHashMap()
        private val params: MutableMap<String, Any?> = Maps.newHashMap()
        private val files: MutableList<Upload> = Lists.newLinkedList()

        private var beforeRequest: ((HttpURLConnection) -> Unit)? = null

        constructor()
        constructor(url: String) {
            this.url = url
        }

        /**
         * 设置Url
         */
        fun url(url: String): Builder {
            this.url = url
            return this
        }

        /**
         * 添加消息体
         */
        fun body(body: Any): Builder {
            if (body is Upload) {
                this.files.add(body)
                return this
            }
            this.body = body
            return this
        }

        /**
         * 添加单个参数
         */
        fun addParam(k: String, v: Any?): Builder {
            this.params[k] = v
            return this
        }

        /**
         * 添加多个参数
         */
        fun addParams(vs: Map<String, Any?>?): Builder {
            if (vs != null) this.params.putAll(vs)
            return this
        }

        /**
         * 添加上传文件
         * @param name 上传对象名
         * @param is 上传对象流
         */
        fun addFile(name: String, `is`: InputStream): Builder {
            this.contentType = MediaType.MULTIPART_FORM_DATA
            this.files.add(Upload(name, `is`))
            return this
        }

        /**
         * 添加单个请求头
         */
        fun addHeader(k: String, v: String): Builder {
            if ("Content-Type".equals(k, ignoreCase = true)) {
                throw XcToolsException("Content-Type不能通过addHeader方法设置, 请使用contentType方法设置")
            }
            this.headers[k] = v
            return this
        }

        /**
         * 添加多个参数
         */
        fun addHeaders(vs: Map<String, String>): Builder {
            if (vs.containsKey("Content-Type")) {
                throw XcToolsException("Content-Type不能通过addHeader方法设置, 请使用contentType方法设置")
            }
            this.headers.putAll(vs)
            return this
        }

        /**
         * 设置请求方式
         */
        fun contentType(contentType: MediaType): Builder {
            this.contentType = contentType
            return this
        }

        /**
         * 添加认证header
         */
        fun authorization(token: String): Builder {
            this.headers["Authorization"] = token
            return this
        }

        /**
         * 添加Basic认证header, 会在token前补齐"Basic "
         */
        fun authorizationBasic(token: String): Builder {
            this.headers["Authorization"] = "Basic $token"
            return this
        }

        /**
         * 添加Basic认证header, 会在token前补齐"Basic "
         */
        fun authorizationBasic(user: String, password: String): Builder {
            val s = Base64.getEncoder().encodeToString("$user:$password".toByteArray())
            return authorizationBasic(s)
        }

        /**
         * 链接超时时间
         */
        fun connectTimeout(seconds: Int): Builder {
            this.connectTimeout = seconds
            return this
        }

        /**
         * 读取超时时间
         */
        fun readTimeout(seconds: Int): Builder {
            this.readTimeout = seconds
            return this
        }

        fun beforeRequest(action: (connection: HttpURLConnection) -> Unit): Builder {
            this.beforeRequest = action
            return this
        }

        fun <T> doGet(function: (uri: String, connection: HttpURLConnection) -> T): T {
            if (url.isBlank()) {
                throw XcToolsException("请求url不能为空")
            }
            if (body != null) {
                log.warn("body will be ignored in Get request")
            }
            var connection: HttpURLConnection? = null
            try {
                val u = if (params.isNotEmpty()) {
                    val query = buildQuery(params)
                    if (url.contains('?')) "$url&$query" else "$url?$query"
                } else url
                connection = initConnection(u, connectTimeout, readTimeout)
                this.beforeRequest?.invoke(connection)
                connection.requestMethod = "GET"

                // 设置header
                headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

                return function(url, connection)
            } catch (e: IOException) {
                throw XcToolsException("请求地址：$url \n\t message: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
        }

        fun <T> doGet(function: (`is`: InputStream) -> T): T = doGet { _, c -> c.inputStream.use(function) }

        fun <T> doGet(url: String, type: Type): T {
            this.url = url
            return doGet { u, c -> getResult(u, c, type) }
        }

        fun <T> doGet(type: Type): T = doGet { u, c -> getResult(u, c, type) }

        fun doGet(url: String): String {
            this.url = url
            return doGet { u, c -> getResult(u, c, String::class.java) }
        }

        fun doGet(): String = doGet { u, c -> getResult(u, c, String::class.java) }

        fun <T> doPost(function: (uri: String, connection: HttpURLConnection) -> T): T {
            if (url.isBlank()) {
                throw XcToolsException("请求url不能为空")
            }
            if (body != null) {
                log.warn("params will be ignored, because body is not null")
            } else {
                body = params
            }
            var connection: HttpURLConnection? = null
            try {
                connection = initPostConnection(url, connectTimeout, readTimeout)
                this.beforeRequest?.invoke(connection)
                // 设置header
                headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

                val bytes: ByteArray
                when (contentType) {
                    MediaType.APPLICATION_FORM_URLENCODED -> {
                        bytes = when (body) {
                            is Map<*, *> -> buildQuery(body as Map<*, *>).toByteArray()
                            is String -> (body as String).toByteArray()
                            is Collection<*> -> throw XcToolsException("body is collection, but not supported")
                            else -> buildQuery(BeanUtils.beanToMap(body)).toByteArray()
                        }
                        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        sendBody(connection, bytes)
                    }

                    MediaType.MULTIPART_FORM_DATA -> {
                        if (body != null) {
                            log.warn("input by body filed will be ignored in FormData request, please use addFile method or addParam(s) method")
                        }
                        val boundary = "----------" + System.currentTimeMillis()
                        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                        // 获得输出流
                        DataOutputStream(connection.outputStream).use { dos ->
                            // 写参数
                            sendParams(params, boundary, dos)
                            // 写文件
                            sendFile(files, boundary, dos)
                            dos.write("$LINE_END$PREFIX$boundary$PREFIX$LINE_END".toByteArray(StandardCharsets.UTF_8))
                            dos.flush()
                        }
                    }

                    MediaType.APPLICATION_XML -> {
                        if (params.isNotEmpty()) {
                            log.warn("input by addParam(s) method will be ignored in XML request, please use body method")
                        }
                        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_ATOM_XML_VALUE)
                        bytes = (body as? String)?.toByteArray() ?: XMLMapper.writeValueAsBytes(body)
                        sendBody(connection, bytes)
                    }

                    else -> {
                        if (params.isNotEmpty()) {
                            log.warn("input by addParam(s) method will be ignored in Json request, please use body method")
                        }
                        // 按照JSON处理
                        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        bytes = (body as? String)?.toByteArray() ?: JSON.toJSONBytes(body)
                        sendBody(connection, bytes)
                    }
                }

                return function(url, connection)
            } catch (e: IOException) {
                throw XcToolsException(
                    "请求地址：$url \n\t message: ${e.message} \n\t body: ${JSON.toJSONString(body)}",
                    e
                )
            } finally {
                connection?.disconnect()
            }
        }

        fun <T> doPost(function: (`is`: InputStream) -> T): T = doPost { _, c -> c.inputStream.use(function) }

        fun <T> doPost(type: Type): T = doPost { u, c -> getResult(u, c, type) }

        fun doPost(): String = doPost { u, c -> getResult(u, c, String::class.java) }
    }

    /**
     * 将map转换为query字符串
     */
    @JvmStatic
    fun buildQuery(params: Map<*, *>): String {
        val list = params.map { "${it.key}=${it.value}" }
        return list.joinToString("&")
    }

    /**
     * 将用户名密码添加到url上
     */
    @JvmStatic
    fun addAuthInUrl(url: String, user: String, password: String): String {
        val u = URL(url)
        var s = u.protocol + "://" +
                URLEncoder.encode(user, StandardCharsets.UTF_8.name()) +
                ':' +
                URLEncoder.encode(password, StandardCharsets.UTF_8.name()) +
                "@" +
                u.authority
        if (u.path != null) s += u.path
        if (u.query != null) s += '?' + u.query
        if (u.ref != null) s += '#' + u.ref
        return s
    }

    private fun <T> getResult(uri: String, connection: HttpURLConnection?, type: Type): T {
        if (connection?.responseCode == HttpStatus.OK.value()) {
            return try {
                if (type == String::class.java) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use(BufferedReader::readText) as T
                } else if (type is Class<*> && InputStream::class.java.isAssignableFrom(type)) {
                    connection.inputStream.use { it as T }
                } else {
                    connection.inputStream.use { JSON.parseObject(it, type) }
                }
            } catch (e: Exception) {
                throw XcToolsException("failed to parse the returned data, uri: $uri \n\t ${e.message}")
            }
        }
        if (connection == null) {
            throw XcToolsException("http connection failed, uri: $uri")
        }
        val err = BufferedReader(InputStreamReader(connection.errorStream)).use(BufferedReader::readText)
        throw XcToolsException("Server Error, response code: ${connection.responseCode}, uri: $uri, message: $err")
    }

    private fun sendBody(connection: HttpURLConnection, body: ByteArray?) {
        if (body?.isNotEmpty() == true) {
            connection.setRequestProperty("Content-Length", body.size.toString())
            connection.outputStream.use { os ->
                os.write(body)
                os.flush()
            }
        }
    }

    private fun sendParams(params: Map<String, Any?>, boundary: String, os: OutputStream) {
        val sb = StringBuilder()
        params.forEach {
            sb.append(PREFIX).append(boundary).append(LINE_END)
            sb.append("Content-Disposition: form-data; name=\"").append(it.key).append("\"").append(LINE_END)
            sb.append("Content-Type: text/plain; charset=utf-8").append(LINE_END)
            sb.append(LINE_END)
            sb.append(it.value)
            sb.append(LINE_END)
        }
        os.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        os.flush()
    }

    private fun sendFile(files: List<Upload>, boundary: String, os: OutputStream) {
        files.forEach {
            val sb = StringBuilder()
            sb.append(PREFIX).append(boundary).append(LINE_END)
            sb.append("Content-Disposition: form-data; name=\"")
                .append(it.fileName).append("\"; filename=\"")
                .append(it.fileName).append("\"")
                .append(LINE_END)
            sb.append("Content-Type:")
                .append(it.type)
                .append(LINE_END)
            sb.append(LINE_END)

            os.write(sb.toString().toByteArray(StandardCharsets.UTF_8))

            DataInputStream(it.`is`).use { dis ->
                dis.copyTo(os)
            }
            os.write(LINE_END.toByteArray(StandardCharsets.UTF_8))
            os.flush()
        }
    }

    private fun initConnection(url: String, connectTimeout: Int = 10, readTimeout: Int = 30): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeout * 1000
        connection.readTimeout = readTimeout * 1000
        return connection
    }

    private fun initPostConnection(url: String, connectTimeout: Int = 10, readTimeout: Int = 30): HttpURLConnection {
        val connection = initConnection(url, connectTimeout, readTimeout)
        connection.setRequestProperty("connection", "Keep-Alive")
        connection.setRequestProperty("Charset", "UTF-8")
        connection.setRequestProperty("Accept", "*/*");
        connection.requestMethod = "POST"
        connection.useCaches = false
        connection.doOutput = true
        connection.doInput = true
        return connection
    }
}
