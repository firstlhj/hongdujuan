package cn.withive.wxpay.callback;

public abstract class GetAccessTokenCallback extends BaseCallback {

    public abstract void success(String accessToken, String openId);
}
