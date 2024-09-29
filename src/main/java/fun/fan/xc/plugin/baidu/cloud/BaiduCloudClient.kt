package `fun`.fan.xc.plugin.baidu.cloud

import com.alibaba.fastjson2.JSON
import `fun`.fan.xc.plugin.baidu.cloud.entity.OcrIdCardBody
import `fun`.fan.xc.plugin.baidu.cloud.entity.OcrIdCardResponse
import `fun`.fan.xc.starter.exception.XcServiceException
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType

class BaiduCloudClient(private val tokenManager: BaiduCloudTokenManager) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 获取百度云的access_token
     */
    fun getToken() = tokenManager.token()

    /**
     * 身份证识别
     */
    fun ocrIdCard(body: OcrIdCardBody): OcrIdCardResponse {
        val response: String = NetUtils.build(BaiduCloudDict.BAIDU_OCR_IDCARD.format(tokenManager.token()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .addParams(body.toRequestBody())
            .doPost()
        log.debug("身份证识别结果: {}", response)
        val result = JSON.parseObject(response, OcrIdCardResponse::class.java)
        return if (result.errorCode != null) {
            log.error("身份证识别错误, 返回信息: {}", response)
            throw XcServiceException(result.errorMsg)
        } else {
            result
        }
    }
}
