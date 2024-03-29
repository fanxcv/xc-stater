package `fun`.fan.xc.plugin.weixin

object WeiXinDict {
    const val WX_BASE_PATH = "/xc/wx"
    const val WX_SUCCESS = "SUCCESS"

    private const val WX_HOST = "https://mp.weixin.qq.com"
    private const val WX_API_HOST = "https://api.weixin.qq.com"
    private const val WX_PAY_HOST = "https://api.mch.weixin.qq.com"

    /**
     * 获取ACCESS_TOKEN
     */
    const val WX_API_ACCESS_TOKEN = "$WX_API_HOST/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s"

    /**
     * 获取稳定版ACCESS_TOKEN
     */
    const val WX_API_STABLE_ACCESS_TOKEN = "$WX_API_HOST/cgi-bin/stable_token"

    /**
     * 获取JS_TICKET
     */
    const val WX_API_JS_TICKET = "$WX_API_HOST/cgi-bin/ticket/getticket?access_token=%s&type=jsapi"

    /**
     * 公众号基础登录
     */
    const val WX_API_LOGIN_BASE =
        "$WX_API_HOST/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code"

    /**
     * 公众号用户信息
     */
    const val WX_API_USER_INFO = "$WX_API_HOST/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN"

    /**
     * 公众号自定义消息
     */
    const val WX_API_MESSAGE_CUSTOM = "$WX_API_HOST/cgi-bin/message/custom/send?access_token=%s"

    /**
     * 公众号模版消息
     */
    const val WX_API_MESSAGE_TEMPLATE = "$WX_API_HOST/cgi-bin/message/template/send?access_token=%s"

    /**
     * 文件上传
     */
    const val WX_API_MEDIA_UPLOAD = "$WX_API_HOST/cgi-bin/media/upload?access_token=%s&type=%s"

    /**
     * 小程序登录
     */
    const val WX_API_CODE_TO_SESSION =
        "$WX_API_HOST/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code"

    /**
     * 下发统一消息, 小程序已废弃
     */
    const val WX_API_MESSAGE_UNIFORM = "$WX_API_HOST/cgi-bin/message/wxopen/template/uniform_send?access_token=%s"

    /**
     * 发送订阅消息
     */
    const val WX_API_MESSAGE_SUBSCRIBE = "$WX_API_HOST/cgi-bin/message/subscribe/send?access_token=%s"

    /**
     * 统一下单接口
     */
    const val WX_API_PAY_UNIFIED_ORDER = "$WX_PAY_HOST/pay/unifiedorder"

    /**
     * 申请退款
     */
    const val WX_API_PAY_REFUND = "$WX_PAY_HOST/secapi/pay/refund"

    /**
     * 委托代扣-支付中签约
     */
    const val WX_API_PAY_CONTRACT_ORDER = "$WX_PAY_HOST/pay/contractorder"

    /**
     * 委托代扣-申请扣款
     */
    const val WX_API_PAY_PAP_APPLY = "$WX_PAY_HOST/pay/pappayapply"

    /**
     * 委托代扣-申请解约
     */
    const val WX_API_PAY_DELETE_CONTRACT = "$WX_PAY_HOST/papay/deletecontract"

    /**
     * 公众号二维码创建
     */
    const val WX_CREATE_QR_CODE = "$WX_API_HOST/cgi-bin/qrcode/create?access_token=%s"

    /**
     * 公众号二维码展示地址
     */
    const val WX_QR_CODE_SHOW = "$WX_HOST/cgi-bin/showqrcode?ticket=%s"

    /**
     * 公众号菜单创建
     */
    const val WX_MENU_CREATE = "$WX_API_HOST/cgi-bin/menu/create?access_token=%s"

    /**
     * 获取不限制的小程序码
     */
    const val MP_UNLIMITED_QR_CODE= "$WX_API_HOST/wxa/getwxacodeunlimit?access_token=%s"
}
