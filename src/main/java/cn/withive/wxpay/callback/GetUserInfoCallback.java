package cn.withive.wxpay.callback;

import cn.withive.wxpay.model.WXUserInfoModel;

public abstract class GetUserInfoCallback extends BaseCallback {
    public abstract void success(WXUserInfoModel userInfo);
}
