package cn.withive.wxpay.model;

import cn.withive.wxpay.constant.OrderTypeEnum;
import lombok.Getter;
import lombok.Setter;

public class OrderModel {

    @Getter
    @Setter
    public String productCode;

    @Getter
    @Setter
    public OrderTypeEnum type;

    @Getter
    @Setter
    private Integer quantity;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String phone;

    @Getter
    @Setter
    public String remark;
}
