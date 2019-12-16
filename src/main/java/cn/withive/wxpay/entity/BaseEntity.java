package cn.withive.wxpay.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
public class BaseEntity implements Serializable {


    @Id
    @Column(name = "id", nullable = false, length = 32)
    @Getter
    @Setter
    private String id;

    @Column(name = "creatTime", updatable = false)
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    private LocalDateTime creatTime;
}
