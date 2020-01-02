package cn.withive.wxpay.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "`Area`")
public class Area {

    @Id
    @Column(nullable = false, length = 32)
    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String name;
}
