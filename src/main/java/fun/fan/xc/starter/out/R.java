package fun.fan.xc.starter.out;

import fun.fan.xc.starter.enums.ReturnCode;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * @author fan
 */
public class R<T> {
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

    @Schema(description = "返回消息体")
    private T body;
    @Schema(description = "返回码, 0为成功")
    private int code;
    @Schema(description = "返回信息")
    private String message;
    @Hidden
    private transient HttpStatus status = HttpStatus.OK;

    private R() {
        code = ReturnCode.SUCCESS.code();
        message = ReturnCode.SUCCESS.message();
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

    public T getBody() {
        return body;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
