package cn.withive.wxpay.config;

import cn.withive.wxpay.sdk.IWXPayDomain;
import cn.withive.wxpay.sdk.WXPayConfig;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 微信支付商户配置
 *
 * @author qiu xiaobing
 * @date 2019/11/14 12:25
 */
@Component
public class WXPayMchConfig extends WXPayConfig {

    @Value("${WX.appId}")
    private String appId;

    @Value("${WX.mchId}")
    private String mchId;

    @Value("${WX.key}")
    private String key;

    @Getter
    @Value("${WX.appSecret}")
    private String appSecret;

    @Getter
    @Value("${WX.SSlCertPath}")
    private String SSlCertPath;

    @Getter
    @Value("${WX.SSlCertPassword}")
    private String SSlCertPassword;

    @Getter
    @Value("${WX.notifyUrl.pay}")
    private String payNotifyUrl;

    @Getter
    @Value("${WX.notifyUrl.refund}")
    private String refundNotifyUrl;

    @Getter
    @Value("${WX.serverUrl}")
    private String serverUrl;

    @Getter
    @Value("${WX.tokenServerUrl}")
    private String tokenServerUrl;

    @Autowired
    private WXPayDomainConfig wxPayDomainConfig;

    private byte[] certData;

    public String getAuthorizeURL() {
        return "https://open.weixin.qq.com/connect/oauth2/authorize";
    }

    public String getAccessTokenURL() {
        return "https://api.weixin.qq.com/sns/oauth2/access_token";
    }

    public String getUserInfoURL() {
        return "https://api.weixin.qq.com/sns/userinfo";
    }

    public String getTokenURL() {
        return "https://api.weixin.qq.com/cgi-bin/token";
    }

    public String getJsApiTicketURL() {
        return "https://api.weixin.qq.com/cgi-bin/ticket/getticket";
    }

    @Override
    public String getAppID() {
        return appId;
    }

    @Override
    public String getMchID() {
        return mchId;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public InputStream getCertStream() {

        if (certData == null) {
            initCert();
        }

        ByteArrayInputStream certBis = new ByteArrayInputStream(this.certData);
        return certBis;
    }

    @Override
    public IWXPayDomain getWXPayDomain() {
        return wxPayDomainConfig;
    }

    private void initCert() {
        File file = new File(SSlCertPath);

        try {
            InputStream certStream = new FileInputStream(file);
            this.certData = new byte[(int) file.length()];
            certStream.read(this.certData);
            certStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
