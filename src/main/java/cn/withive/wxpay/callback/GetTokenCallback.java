package cn.withive.wxpay.callback;

public abstract class GetTokenCallback extends BaseCallback {

    public abstract void success(String accessToken, Long expiresIn);
}
