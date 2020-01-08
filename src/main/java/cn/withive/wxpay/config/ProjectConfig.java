package cn.withive.wxpay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProjectConfig {

    @Value("${user.sync_info_day}")
    @Getter
    public Integer syncUserInfoDay;

    @Value("${project.url}")
    @Getter
    protected String serverUrl;

    @Value("${order.create_frequency_value}")
    @Getter
    private Long createFrequencyValue;

    @Value("${order.create_frequency_expire}")
    @Getter
    private Long createFrequencyExpire;
}
