package cn.withive.wxpay.entity;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.OrderTypeEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "`Order`", uniqueConstraints = {@UniqueConstraint(columnNames = "code")})
public class Order extends BaseEntity {

    @Getter
    @Setter
    @Column(length = 32)
    private String code;

    @Getter
    @Setter
    @Column(length = 50)
    private String wechatOpenId;

    @Getter
    @Setter
    @Column(length = 32)
    private String productId;

    @Getter
    @Setter
    private String productName;

    @Getter
    @Setter
    private BigDecimal amount;

    @Getter
    @Setter
    private Integer quantity;

    @Getter
    @Setter
    private OrderTypeEnum type;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @Column(length = 50)
    private String phone;

    @Getter
    @Setter
    private OrderStatusEnum status;

    @Getter
    @Setter
    private LocalDateTime payTime;

    @Getter
    @Setter
    private String remark;

    @Getter
    @Setter
    @Column(length = 32)
    private String areaCode;
}
