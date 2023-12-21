package `fun`.fan.xc.plugin.baidu.cloud

object BaiduCloudDict {
    private const val BAIDU_CLOUD_HOST = "https://aip.baidubce.com"

    /**
     * 获取AccessToken
     */
    const val BAIDU_CLOUD_ACCESS_TOKEN = "${BAIDU_CLOUD_HOST}/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials"

}
