package cn.withive.wxpay.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
public class BaseEntity implements Serializable {


    @Id
    @Column(name = "id", nullable = false)
    @Size(max = 32)
    @Getter
    @Setter
    private String id;

    @Column(name = "creatTime", updatable = false)
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    @Getter
    @Setter
    private LocalDateTime creatTime;
}
