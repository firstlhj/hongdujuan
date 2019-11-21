package cn.withive.wxpay.callback;

public abstract class GetJsApiTicketCallback extends BaseCallback {

    public abstract void success(String ticket, Long expiresIn);
}
