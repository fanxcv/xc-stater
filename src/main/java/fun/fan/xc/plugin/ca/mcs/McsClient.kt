package `fun`.fan.xc.plugin.ca.mcs

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.TypeReference
import com.alibaba.fastjson2.toJSONString
import `fun`.fan.xc.plugin.ca.mcs.annotations.McsPath
import `fun`.fan.xc.plugin.ca.mcs.entity.McsBody
import `fun`.fan.xc.plugin.ca.mcs.entity.Request
import `fun`.fan.xc.plugin.ca.mcs.entity.Response
import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Type

class McsClient(val config: McsConfig) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun <T : McsBody, V> execute(body: T, type: Type): V {
        // 获取body对象上的McsPath注解
        val mcsPath = body::class.java.getAnnotation(McsPath::class.java) ?: throw XcServiceException("reqBody对象上没有McsPath注解, 无法获取接口地址")
        val url = mcsPath.value

        if (url.isBlank()) {
            throw XcServiceException("接口地址不能配置为空")
        }

        val request: Request<T> = Request.Builder<T>()
            .setSerialNo(body.serialNo)
            .setAttach(body.attach)
            .setReqBody(body)
            .build()

        val json = JSON.toJSONString(request)

        if (config.isDebug) {
            log.debug("请求地址: {}\n参数: {}", url, json)
        }

        return NetUtils.build("${config.apiHost}$url")
            .body(json)
            .doPost { it ->
                val response: Response<JSONObject> = JSON.parseObject(it, object : TypeReference<Response<JSONObject?>?>() {}.type)
                if (config.isDebug) {
                    log.debug("请求地址: {}\n返回: {}", url, response.toJSONString())
                }
                if (response.resHead?.code != "200") {
                    log.error("接口调用失败: {}", response.toJSONString())
                    throw XcServiceException(response.resHead?.message ?: "接口调用失败")
                }
                response.resBody.to(type)
            }
    }
}
