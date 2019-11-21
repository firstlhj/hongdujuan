package cn.withive.wxpay;

import cn.withive.wxpay.sdk.JsApi.WXJsApiUtil;
import cn.withive.wxpay.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class SignTest {

    @Test
    void test1() {

        String sign = WXJsApiUtil.generateSignature("Wm3WZYTPz0wzccnW",
                "1414587457",
                "sM4AOVdWfPE4DxkXGEs8VMCPGGVi4C3VM0P37wVUCFvkVAy_90u5h9nbSlYy3-Sl-HhTdfl2fzFy1AOcHKP7qg",
                "http://mp.weixin.qq.com?params=value");

        log.info(sign.toString());
    }

    @Test
    void test2() {
        String str = RandomUtil.generateUniqueStr();
        System.out.println(str);
    }
}
