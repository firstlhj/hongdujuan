package cn.withive.wxpay.service;

import cn.withive.wxpay.callback.GetAccessTokenCallback;
import cn.withive.wxpay.callback.GetUserInfoCallback;
import cn.withive.wxpay.config.WXPayMchConfig;
import cn.withive.wxpay.constant.BillTypeEnum;
import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.exception.TokenExpireException;
import cn.withive.wxpay.exception.WxException;
import cn.withive.wxpay.model.ResModel;
import cn.withive.wxpay.model.WXUserTokenModel;
import cn.withive.wxpay.model.WXUserInfoModel;
import cn.withive.wxpay.sdk.JsApi.WXJsApiUtil;
import cn.withive.wxpay.sdk.WXPay;
import cn.withive.wxpay.sdk.WXPayConstants;
import cn.withive.wxpay.sdk.WXPayUtil;
import cn.withive.wxpay.util.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WXService {

    @Autowired
    private WXPayMchConfig config;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Long tokenExpire = 5400L;

    public WXPayMchConfig getConfig() {
        return config;
    }

    /**
     * <p>
     * 微信文档地址：
     * <a href="https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html">
     * 构造网页授权获取code的URL</a>
     * </P>
     *
     * @param redirectUri
     * @param scope
     * @param state
     * @return
     */
    public String getAuthorizeUrl(String redirectUri, String scope, String state) {
        if (state == null) {
            state = "";
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("appid", config.getAppID());
        data.put("redirect_uri", HttpUtil.UrlEncode(redirectUri));
        data.put("response_type", "code");
        data.put("scope", scope);
        data.put("state", state + "#wechat_redirect");
        String url = config.getAuthorizeURL() + HttpUtil.toUrl(data);

        return url;
    }

    /**
     * 获取授权数据
     *
     * @param code
     * @param callback
     */
    public void getUserToken(@NonNull String code, @NonNull GetAccessTokenCallback callback) {
        if (StringUtils.isEmptyOrWhitespace(code)) {
            throw new IllegalArgumentException("缺少必要参数：code！");
        }
        if (callback == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("appid", config.getAppID());
        data.put("secret", config.getAppSecret());
        data.put("code", code);
        data.put("grant_type", "authorization_code");
        String url = config.getAccessTokenURL() + HttpUtil.toUrl(data);

        HttpUtil.HttpGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                log.error(e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String result = response.body().string();
                JSONObject resultObj = JSON.parseObject(result);

                if (resultObj.containsKey("errcode")) {
                    callback.failure(result);
                    return;
                }

                String accessToken = resultObj.getString("access_token");
                String openId = resultObj.getString("openid");

                callback.success(accessToken, openId);
            }
        });
    }

    public void setAccessTokenToCache(@NonNull String openId, @NonNull String token) {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            throw new IllegalArgumentException("缺少查询必要参数：openId！");
        }
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        valueOperations.set(CacheKeyConstEnum.token_user_key.getKey(openId), token);
    }

    public @Nullable
    WXUserTokenModel getUserToken(@NonNull String code) {
        if (StringUtils.isEmptyOrWhitespace(code)) {
            throw new IllegalArgumentException("缺少必要参数：code！");
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("appid", config.getAppID());
        data.put("secret", config.getAppSecret());
        data.put("code", code);
        data.put("grant_type", "authorization_code");
        String url = config.getAccessTokenURL() + HttpUtil.toUrl(data);

        String body = HttpUtil.HttpGet(url);
        JSONObject jsonObject = JSON.parseObject(body);

        if (jsonObject.containsKey("errcode")) {
            return null;
        }

        WXUserTokenModel result = jsonObject.toJavaObject(WXUserTokenModel.class);

        // 缓存access_token，便于以后再获取用户信息
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(CacheKeyConstEnum.token_user_key.getKey(result.getOpenid()), result.getAccess_token(),
                result.getExpires_in(), TimeUnit.SECONDS);

        // TODO: 缓存住refresh_token

        return result;
    }

    public String getUserTokenFormCache(@NonNull String openId) {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            throw new IllegalArgumentException("缺少查询必要参数：openId！");
        }

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        String token = valueOperations.get(CacheKeyConstEnum.token_user_key.getKey(openId));

        if (StringUtils.isEmpty(token)) {
            // TODO: 刷新获取token
        }

        return token;
    }

    /**
     * 获取用户信息
     *
     * @param accessToken
     * @param openId
     * @param callback
     */
    public void getUserInfo(@NonNull String accessToken, @NonNull String openId,
                            @NonNull GetUserInfoCallback callback) {
        if (StringUtils.isEmptyOrWhitespace(accessToken)) {
            throw new IllegalArgumentException("缺少必要参数：accessToken！");
        }
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            throw new IllegalArgumentException("缺少必要参数：openId！");
        }
        if (callback == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("openid", openId);
        data.put("lang", "zh_CN");
        String url = config.getUserInfoURL() + HttpUtil.toUrl(data);

        HttpUtil.HttpGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                log.error(e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String result = response.body().string();
                JSONObject resultObj = JSON.parseObject(result);

                if (resultObj.containsKey("errcode")) {
                    callback.failure(result);
                    return;
                }

                WXUserInfoModel userInfo = resultObj.toJavaObject(WXUserInfoModel.class);

                callback.success(userInfo);
            }
        });
    }

    public @Nullable
    WXUserInfoModel getUserInfo(@NonNull String accessToken, @NonNull String openId) {
        if (StringUtils.isEmptyOrWhitespace(accessToken)) {
            throw new IllegalArgumentException("缺少必要参数：accessToken！");
        }
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            throw new IllegalArgumentException("缺少必要参数：openId！");
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("openid", openId);
        data.put("lang", "zh_CN");
        String url = config.getUserInfoURL() + HttpUtil.toUrl(data);

        String body = HttpUtil.HttpGet(url);

        JSONObject jsonObject = JSON.parseObject(body);

        if (jsonObject.containsKey("errcode")) {
            return null;
        }

        WXUserInfoModel userInfo = jsonObject.toJavaObject(WXUserInfoModel.class);

        return userInfo;
    }

    public WXUserInfoModel getUserInfo(@NonNull String openId) throws WxException, TokenExpireException {
        if (StringUtils.isEmptyOrWhitespace(openId)) {
            throw new IllegalArgumentException("缺少必要参数：openId！");
        }

        String token = this.getUserTokenFormCache(openId);
        if (StringUtils.isEmpty(token)) {
            // token 失效了
            throw new TokenExpireException();
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("access_token", token);
        data.put("openid", openId);
        data.put("lang", "zh_CN");
        String url = config.getUserInfoURL() + HttpUtil.toUrl(data);

        String body = HttpUtil.HttpGet(url);

        JSONObject jsonObject = JSON.parseObject(body);

        if (jsonObject.containsKey("errcode")) {
            throw new WxException();
        }

        WXUserInfoModel userInfo = jsonObject.toJavaObject(WXUserInfoModel.class);

        return userInfo;
    }

    /**
     * 统一下单接口
     *
     * @return
     */
    public Map<String, String> getUnifiedOrderResult(BigDecimal totalFee, String outTradeNo, String spbillCreateIp,
                                                     String openId) {
        WXPay wxpay = new WXPay(config);

        totalFee = totalFee.multiply(new BigDecimal(100));
        LocalDateTime timeStart = LocalDateTime.now();
        LocalDateTime timeExpire = timeStart.plusHours(2);

        Map<String, String> data = new HashMap<>();
        data.put("body", "种植红杜鹃");
        data.put("attach", "公益活动");
        data.put("out_trade_no", outTradeNo);
        data.put("total_fee", totalFee.stripTrailingZeros().toPlainString());
//        data.put("spbill_create_ip", spbillCreateIp);
        data.put("spbill_create_ip", "123.12.12.123");
        data.put("time_start", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(timeStart));
        data.put("time_expire", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(timeExpire));
        data.put("notify_url", config.getPayNotifyUrl());
        data.put("trade_type", "JSAPI");
        data.put("openid", openId);

        try {
            Map<String, String> resp = wxpay.unifiedOrder(data);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 查询订单
     *
     * @param transactionId 微信订单号
     * @return
     */
    public @Nullable
    Map<String, String> orderQueryById(String transactionId) {
        WXPay wxpay = new WXPay(config);
        Map<String, String> data = new HashMap<>();
        data.put("transaction_id", transactionId);

        try {
            Map<String, String> result = wxpay.orderQuery(data);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查询订单
     *
     * @param outTradeNo 商户订单号
     * @return
     */
    public @Nullable
    Map<String, String> orderQueryByCode(String outTradeNo) {
        WXPay wxpay = new WXPay(config);
        Map<String, String> data = new HashMap<>();
        data.put("out_trade_no", outTradeNo);

        try {
            Map<String, String> result = wxpay.orderQuery(data);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成调用H5支付参数数据
     *
     * @param prepayId
     * @return
     */
    public @Nullable
    Map<String, String> getJsApiPayParameters(String prepayId) {
        Map<String, String> jsApiParam = new LinkedHashMap<>();
        jsApiParam.put("appId", config.getAppID());
        jsApiParam.put("timeStamp", String.valueOf(WXPayUtil.getCurrentTimestamp()));
        jsApiParam.put("nonceStr", WXPayUtil.generateNonceStr());
        jsApiParam.put("package", "prepay_id=" + prepayId);
        jsApiParam.put("signType", WXPayConstants.HMACSHA256);

        try {
            String sign = WXPayUtil.generateSignature(jsApiParam, config.getKey(), WXPayConstants.SignType.HMACSHA256);
            jsApiParam.put("paySign", sign);
            return jsApiParam;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取全局唯一接口调用凭据，对接中控服务器接口
     * 有效期7200秒，会缓存
     */
    public @Nullable
    String getGlobalToken() {

        String accessToken = stringRedisTemplate.opsForValue().get(CacheKeyConstEnum.token_global_key.getKey());

        if (!StringUtils.isEmpty(accessToken)) {
            return accessToken;
        }

        if (!StringUtils.isEmptyOrWhitespace(config.getTokenServerUrl())) {
            // 对接中控服务器接口获取微信token

            String result = HttpUtil.HttpGet(config.getTokenServerUrl());
            if (result == null) {
                return null;
            }

            ResModel resultObj = JSON.parseObject(result, ResModel.class);

            if (resultObj.getCode().equals(ResModel.StatusEnum.FAILURE)) {
                return null;
            }

            accessToken = (String) resultObj.getData();

            stringRedisTemplate.opsForValue().set(CacheKeyConstEnum.token_global_key.getKey(), accessToken, tokenExpire,
                    TimeUnit.SECONDS);

            return accessToken;
        }

        // 缓存中不存在token，那么向微信申请
        Map<String, String> data = new LinkedHashMap<>();
        data.put("grant_type", "client_credential");
        data.put("appid", config.getAppID());
        data.put("secret", config.getAppSecret());
        String url = config.getTokenURL() + HttpUtil.toUrl(data);

        String result = HttpUtil.HttpGet(url);

        if (result == null) {
            return null;
        }

        JSONObject resultObj = JSON.parseObject(result);

        if (resultObj.containsKey("errcode")) {
            return null;
        }

        accessToken = resultObj.getString("access_token");
        Long expiresIn = resultObj.getLong("expires_in");

        stringRedisTemplate.opsForValue().set(CacheKeyConstEnum.token_global_key.getKey(), accessToken, tokenExpire,
                TimeUnit.SECONDS);

        return accessToken;
    }

    /**
     * 获取调用微信JS接口的临时票据
     * 有效期7200秒，需缓存
     *
     * @return
     */
    public String getJsApiTicket(String accessToken) {
        String ticket = stringRedisTemplate.opsForValue().get(CacheKeyConstEnum.token_ticket_key.getKey());

        if (!StringUtils.isEmpty(ticket)) {
            return ticket;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("access_token", accessToken);
        data.put("type", "jsapi");
        String url = config.getJsApiTicketURL() + HttpUtil.toUrl(data);

        String result = HttpUtil.HttpGet(url);

        if (result == null) {
            return null;
        }

        JSONObject resultObj = JSON.parseObject(result);

        if (!resultObj.getString("errmsg").equals("ok")) {
            return null;
        }

        ticket = resultObj.getString("ticket");

        stringRedisTemplate.opsForValue().set(CacheKeyConstEnum.token_ticket_key.getKey(), ticket, tokenExpire,
                TimeUnit.SECONDS);

        return ticket;
    }

    /**
     * 获取js api权限验证配置
     *
     * @param url 当前网页的URL，不包含#及其后面部分
     * @return
     */
    public @Nullable
    Map<String, String> getJsApiConfig(String url) {
        String accessToken = this.getGlobalToken();
        if (StringUtils.isEmpty(accessToken)) {
            return null;
        }

        String ticket = this.getJsApiTicket(accessToken);
        if (StringUtils.isEmpty(ticket)) {
            return null;
        }

        String nonceStr = WXPayUtil.generateNonceStr();
        String timeStamp = String.valueOf(WXPayUtil.getCurrentTimestamp());

        // 配置字段
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("appId", config.getAppID());
        apiConfig.put("timestamp", timeStamp);
        apiConfig.put("nonceStr", nonceStr);

        try {
            String sign = WXJsApiUtil.generateSignature(nonceStr, timeStamp, ticket, url);
            apiConfig.put("signature", sign);
            return apiConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public @Nullable
    String downloadBill(LocalDate billDate, BillTypeEnum billType) {
        WXPay wxpay = new WXPay(config);

        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(billDate);

        Map<String, String> reqData = new LinkedHashMap<>();
        reqData.put("bill_date", date);
        reqData.put("bill_type", billType.name());

        String bill = null;

        try {
            Map<String, String> result = wxpay.downloadBill(reqData);
            if (!"ok".equals(result.get("return_msg"))) {
                return null;
            }

            bill = result.get("data");
            return bill;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

    public @Nullable
    String downloadBill(LocalDate billDate, String folder, BillTypeEnum billType) {

        // 创建父目录
        folder = String.format("%s\\%s", folder, billType.name());
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(billDate);
        String path = String.format("%s\\%s.csv", folder, date);

        File file = new File(path);
        if (file.exists()) {
            // 账单文件存在
            return file.toString();
        }

        try {
            File directory = new File(folder);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 创建指定文件
            String content = this.downloadBill(billDate, billType);

            if (StringUtils.isEmpty(content)) {
                return null;
            }

            file.createNewFile();

            content = content.replaceAll("`", "");

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

            log.info("账单路径为：{}", path);

            return content;
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}
