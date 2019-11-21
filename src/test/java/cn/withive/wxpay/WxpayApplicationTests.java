package cn.withive.wxpay;

import cn.withive.wxpay.callback.GetAccessTokenCallback;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.service.WXService;
import cn.withive.wxpay.util.HttpUtil;
import cn.withive.wxpay.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@SpringBootTest
class WxpayApplicationTests {

    @Autowired
    private WXService WXService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void test1() throws UnsupportedEncodingException {
        String url = URLEncoder.encode("http://localhost/index.html", "UTF-8");
        log.info(url);
    }

    @Test
    void test2() {
        String result = HttpUtil.HttpGet("https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET" +
                "&code" +
                "=CODE" +
                "&grant_type=authorization_code");

        log.info(result);
    }

    @Test
    void test3() {
        WXService.getAccessToken("12345", new GetAccessTokenCallback() {
            @Override
            public void success(String accessToken, String openId) {
                log.info(accessToken);
                log.info(openId);
            }

            @Override
            public void failure(String rawData) {
                log.info(rawData);
            }
        });
    }

    @Test
    void test4() {
        for (int i = 0; i < 10000; i++) {
//            String uniqueStr = RandomUtil.generateUniqueStr();
            String uniqueStr = RandomUtil.generateUniqueStr();
            System.out.print(i + ":");
            System.out.println(uniqueStr);
//            log.info(uniqueStr);
        }
    }

    @Test
    void test5() {
        String time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        log.info(time);
    }

    @Test
    void test6() {
    }

    @Test
    void test7() {
        Product product = new Product();
        product.setName("小树");
        product.setCode("001");
        product.setAmount(new BigDecimal(0.01));

        productRepository.save(product);
    }
}
