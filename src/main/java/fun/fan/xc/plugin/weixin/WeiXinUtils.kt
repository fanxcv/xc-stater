package `fun`.fan.xc.plugin.weixin

import cn.hutool.core.codec.Base64
import com.alibaba.fastjson2.JSONObject
import com.fasterxml.jackson.annotation.JsonProperty
import `fun`.fan.xc.plugin.weixin.entity.PayBase
import `fun`.fan.xc.plugin.weixin.entity.PayRefundNotifyResp
import `fun`.fan.xc.plugin.weixin.token.WeiXinTokenManager
import `fun`.fan.xc.starter.utils.BeanUtils
import `fun`.fan.xc.starter.utils.EncryptUtils
import `fun`.fan.xc.starter.utils.NetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.Assert
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object WeiXinUtils {
  private val log: Logger = LoggerFactory.getLogger(this::class.java)

  /**
   * 微信签名
   */
  fun sign(params: Map<String, Any?>, key: String): String {
    val sb = StringBuilder()
    params.filter { it.value != null && it.value.toString() != "" }
      .map { it.key to it.value }
      .sortedBy { it.first }
      .forEach { sb.append("${it.first}=${it.second}&") }
    sb.append("key=$key")
    return EncryptUtils.md5(sb.toString()).uppercase(Locale.getDefault())
  }

  fun sign(params: PayBase, key: String) {
    if (params.sign.isNullOrBlank()) {
      val map = BeanUtils.beanToMap(params) {
        val property = it.getAnnotation(JsonProperty::class.java)
        property?.value ?: it.name
      }
      val sign = sign(map, key)
      params.sign = sign
    }
  }

  fun decodeRefundInfo(reqInfo: String, key: String): PayRefundNotifyResp.RefundInfo {
    val bytes = Base64.decode(reqInfo)
    val keySpec = SecretKeySpec(EncryptUtils.md5(key).lowercase(Locale.getDefault()).toByteArray(), "AES")
    val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec)
    val buffer = cipher.doFinal(bytes)
    return NetUtils.XMLMapper.readValue(buffer, PayRefundNotifyResp.RefundInfo::class.java)
  }

  fun parseAndUpdateToken(
    entity: WeiXinTokenManager.BaseTokenEntity, key: String,
    tokenKey: String = "access_token", expiresKey: String = "expires_in",
    json: String
  ) {
    // 先判断是否正确获取到tokenKey了
    Assert.isTrue(json.contains(tokenKey), "$key: 请求Token失败了, response: $json")

    val map: JSONObject = JSONObject.parse(json)
    entity.token = map.getString(tokenKey)

    // map["expires"]是处理client端的
    val expiresTime = map.getLong(expiresKey)
    Assert.isTrue(expiresTime != null && expiresTime > 0L, "$key: 获取到的Token无效: $json")

    val now = System.currentTimeMillis()
    if (expiresTime > now) {
      // 如果到期时间大于当前时间, 证明已经被server算过了
      entity.expires = expiresTime
      entity.refresh = expiresTime - 500L * 1000L
    } else {
      // 提前30秒就触发同步刷新
      entity.expires = now + (expiresTime - 30L) * 1000L
      // 提前十分钟就进行刷新操作
      entity.refresh = now + (expiresTime - 600L) * 1000L
    }

    log.info("{}: 获取到新的Token: {}, 到期时间: {}", key, entity.token, entity.expires)
  }
}
