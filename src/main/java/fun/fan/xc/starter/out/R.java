package fun.fan.xc.starter.out;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import fun.fan.xc.starter.enums.ReturnCode;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * @author fan
 */
@Data
public class R<T> {
    @Hidden
    @JsonIgnore
    @JSONField(serialize = false)
    private final transient Map<String, String> cookies = Maps.newHashMap();
    @Hidden
    @JsonIgnore
    @JSONField(serialize = false)
    private final transient Map<String, String> headers = Maps.newHashMap();
    @Hidden
    @JsonIgnore
    @JSONField(serialize = false)
    private final transient Map<String, Object> extendData = Maps.newHashMap();
    @Schema(description = "返回消息体")
    private T body;
    @Schema(description = "返回码, 0为成功")
    private int code;
    @Schema(description = "返回信息")
    private String message;
    @Hidden
    @JsonIgnore
    @JSONField(serialize = false)
    private transient HttpStatus status = HttpStatus.OK;

    private R() {
        code = ReturnCode.SUCCESS.code();
        message = ReturnCode.SUCCESS.message();
    }

    public static <D> R<D> build() {
        return success();
    }

    public static <D> R<D> success() {
        return new R<D>().returnCode(ReturnCode.SUCCESS);
    }

    public static <D> R<D> success(D data) {
        return new R<D>().returnCode(ReturnCode.SUCCESS).body(data);
    }

    public static <D> R<D> fail() {
        return new R<D>().returnCode(ReturnCode.FAIL);
    }

    public static <D> R<D> fail(ReturnCode rc) {
        return new R<D>().returnCode(rc);
    }

    public static <D> R<D> fail(int code, String msg) {
        return new R<D>().code(code).message(msg).status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <D> R<D> fail(String msg) {
        return new R<D>().returnCode(ReturnCode.FAIL).message(msg);
    }

    public R<T> returnCode(ReturnCode rc) {
        this.message = rc.message();
        this.status = rc.status();
        this.code = rc.code();
        return this;
    }

    public R<T> code(int code) {
        this.code = code;
        return this;
    }

    public R<T> message(String message) {
        this.message = message;
        return this;
    }

    public R<T> body(T body) {
        this.body = body;
        return this;
    }

    public R<T> status(HttpStatus status) {
        this.status = status;
        return this;
    }

    /**
     * 添加header
     *
     * @param name  header名
     * @param value 值
     * @return 本对象
     */
    public R<T> header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * 添加cookie
     *
     * @param key   key
     * @param value 值
     * @return 本对象
     */
    public R<T> cookie(String key, String value) {
        this.cookies.put(key, value);
        return this;
    }

    /**
     * 添加与code平级的数据, 无法覆盖code,body,message
     *
     * @param key   返回key
     * @param value 返回值
     */
    public R<T> set(String key, Object value) {
        this.extendData.put(key, value);
        return this;
    }
}
