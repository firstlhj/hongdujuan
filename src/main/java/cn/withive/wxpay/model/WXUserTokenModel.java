package cn.withive.wxpay.model;

import lombok.Getter;
import lombok.Setter;

public class WXUserTokenModel {

    @Getter
    @Setter
    private String access_token;

    @Getter
    @Setter
    private Long expires_in;

    @Getter
    @Setter
    private String refresh_token;

    @Getter
    @Setter
    private String openid;

    @Getter
    @Setter
    private String scope;
}
