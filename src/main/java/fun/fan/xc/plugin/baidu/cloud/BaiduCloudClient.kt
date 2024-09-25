package `fun`.fan.xc.plugin.baidu.cloud

import `fun`.fan.xc.plugin.baidu.cloud.entity.OcrIdCardBody
import `fun`.fan.xc.plugin.baidu.cloud.entity.OcrIdCardResponse
import `fun`.fan.xc.starter.utils.NetUtils
import org.springframework.http.MediaType

class BaiduCloudClient(private val tokenManager: BaiduCloudTokenManager) {
    /**
     * 获取百度云的access_token
     */
    fun getToken() = tokenManager.token()

    /**
     * 身份证识别
     */
    fun ocrIdCard(body: OcrIdCardBody): OcrIdCardResponse {
        return NetUtils.build(BaiduCloudDict.BAIDU_OCR_IDCARD.format(tokenManager.token()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .addParams(body.toRequestBody())
            .respType(OcrIdCardResponse::class.java)
            .doPost()
    }
}
