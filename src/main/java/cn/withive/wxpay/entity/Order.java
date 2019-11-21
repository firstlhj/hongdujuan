package cn.withive.wxpay.entity;

import cn.withive.wxpay.constant.OrderStatusEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "`Order`")
public class Order extends BaseEntity {

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String wechatOpenId;

    @Getter
    @Setter
    @Size(max = 32)
    private String productId;

    @Getter
    @Setter
    private String productName;

    @Getter
    @Setter
    private BigDecimal amount;

    @Getter
    @Setter
    private OrderStatusEnum status;

    @Getter
    @Setter
    private LocalDateTime payTime;

    @Getter
    @Setter
    private String remark;
}
