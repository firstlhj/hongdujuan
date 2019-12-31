package cn.withive.wxpay.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "Product", uniqueConstraints = {@UniqueConstraint(columnNames = "code")})
public class Product extends BaseEntity {

    @Getter
    @Setter
    @Column(length = 32)
    private String code;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private BigDecimal amount;
}
