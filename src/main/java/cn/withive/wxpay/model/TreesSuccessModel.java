package cn.withive.wxpay.model;

import lombok.Getter;
import lombok.Setter;

public class TreesSuccessModel {

    /**
     * 用户排名
     */
    @Getter
    @Setter
    private Long rank;

    /**
     * 头像链接
     */
    @Getter
    @Setter
    private String avatar;

    @Getter
    @Setter
    private String nickname;
}
