package cn.withive.wxpay.config;

import cn.withive.wxpay.sdk.IWXPayDomain;
import cn.withive.wxpay.sdk.WXPayConfig;
import org.springframework.stereotype.Component;

@Component
public class WXPayDomainConfig implements IWXPayDomain {

    public static final String DOMAIN_API = "api.mch.weixin.qq.com";
    public static final String DOMAIN_API2 = "api2.mch.weixin.qq.com";
    public static final String DOMAIN_APIHK = "apihk.mch.weixin.qq.com";
    public static final String DOMAIN_APIUS = "apius.mch.weixin.qq.com";

    @Override
    public void report(String domain, long elapsedTimeMillis, Exception ex) {

    }

    @Override
    public DomainInfo getDomain(WXPayConfig config) {
        DomainInfo info = new DomainInfo(DOMAIN_API, true);

        return info;
    }
}
