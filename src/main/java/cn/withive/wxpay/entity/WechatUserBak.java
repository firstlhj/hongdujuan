package cn.withive.wxpay.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

//@Entity
//@Table(name = "WechatUserBak", uniqueConstraints = {@UniqueConstraint(columnNames = "openId")})
public class WechatUserBak extends BaseEntity {

    @Getter
    @Setter
    @Column(length = 50)
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
