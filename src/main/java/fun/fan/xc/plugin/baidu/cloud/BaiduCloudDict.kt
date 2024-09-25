package `fun`.fan.xc.plugin.baidu.cloud

object BaiduCloudDict {
    private const val BAIDU_CLOUD_HOST = "https://aip.baidubce.com"

    /**
     * 获取AccessToken
     */
    const val BAIDU_CLOUD_ACCESS_TOKEN = "${BAIDU_CLOUD_HOST}/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials"

    /**
     * 身份证识别
     */
    const val BAIDU_OCR_IDCARD = "${BAIDU_CLOUD_HOST}/rest/2.0/ocr/v1/idcard?access_token=%s"
}
