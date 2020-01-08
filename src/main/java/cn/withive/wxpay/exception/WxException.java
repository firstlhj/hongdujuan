package cn.withive.wxpay.exception;

/**
 * 自定义微信异常类
 * @author qiu xiaobing
 * @date 2019/11/15 12:55
 */
public class WxException extends Exception {
    private final static String message = "获取微信用户信息异常";

    public WxException() {
        super(message);
    }

    public WxException(String message) {
        super(message);
    }
}
