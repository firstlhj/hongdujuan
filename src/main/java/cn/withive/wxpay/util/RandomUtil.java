package cn.withive.wxpay.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class RandomUtil {

    private static final String SYMBOLS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new SecureRandom();

    /**
     * 获取随机字符串 Nonce Str
     *
     * @return String 随机字符串
     */
    public static String generateNonceStr() {
        char[] nonceChars = new char[32];
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        return new String(nonceChars);
    }

    public static String generateUniqueStr() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");

        return uuid;
    }
}
