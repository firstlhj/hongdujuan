package cn.withive.wxpay.model;

import lombok.Getter;
import lombok.Setter;

public class UserModel {

    @Getter
    @Setter
    private String signature;

    @Getter
    @Setter
    private String realName;

    @Getter
    @Setter
    private String phone;
}
