package cn.withive.wxpay.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @date 2019/11/15 17:24
 */
public class ResModel {

    @Getter
    @Setter
    private Object data;

    @Getter
    @Setter
    private String msg;

    @Getter
    @Setter
    private StatusEnum code = StatusEnum.SUCCESS;

    public enum StatusEnum {
        FAILURE,
        SUCCESS
    }
}
