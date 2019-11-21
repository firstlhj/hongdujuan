package cn.withive.wxpay.util;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Slf4j
public class HttpUtil {

    /**
     * 使用utf-8格式格式化url
     *
     * @param str
     * @return
     */
    public static String UrlEncode(String str) {
        if (str != null) {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String toUrl(Map<String, String> map) {
        StringBuilder buff = new StringBuilder();

        buff.append("?");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                log.error("WxPayData内部含有值为null的字段!");
                throw new IllegalArgumentException("WxPayData内部含有值为null的字段!");
            }

            if (!key.equals("sign") && !value.equals("")) {
                buff.append(key).append("=").append(value).append("&");
            }
        }
        // 删除最后一个 &
        buff.deleteCharAt(buff.length() - 1);

        return buff.toString();
    }

    public static void HttpGet(String url, Callback callback) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(callback);
    }

    public static String HttpGet(String url) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
