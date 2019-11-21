package cn.withive.wxpay.exception;

/**
 * 自定义微信异常类
 * @author qiu xiaobing
 * @date 2019/11/15 12:55
 */
public class WxPayException extends RuntimeException {
    public WxPayException() {
        super();
    }

    public WxPayException(String message) {
        super(message);
    }
}
