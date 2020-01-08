package cn.withive.wxpay.exception;

/**
 * @author qiu xiaobing
 * @date 2019/11/15 12:55
 */
public class TokenExpireException extends Exception {
    private final static String message = "token已过期";

    public TokenExpireException() {
        super(message);
    }

    public TokenExpireException(String message) {
        super(message);
    }
}
