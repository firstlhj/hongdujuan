package cn.withive.wxpay.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class WXUserInfoModel {

    @Getter
    @Setter
    private String openid;

    @Getter
    @Setter
    private String nickname;

    @Getter
    @Setter
    private String sex;

    @Getter
    @Setter
    private String province;

    @Getter
    @Setter
    private String city;

    @Getter
    @Setter
    private String country;

    @Getter
    @Setter
    private String headimgurl;

    @Getter
    @Setter
    private List<String> privilege;

    @Getter
    @Setter
    private String unionid;


}
