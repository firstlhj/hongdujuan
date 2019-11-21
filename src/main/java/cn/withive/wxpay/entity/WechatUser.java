package cn.withive.wxpay.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "WechatUser")
public class WechatUser extends BaseEntity {

    @Getter
    @Setter
    private String openId;

    @Getter
    @Setter
    private String nickname;

    @Getter
    @Setter
    private String avatar;

    @Getter
    @Setter
    private String country;

    @Getter
    @Setter
    private String province;

    @Getter
    @Setter
    private String city;
}
