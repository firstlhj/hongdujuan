package cn.withive.wxpay;

import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.service.OrderService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.thymeleaf.util.StringUtils;

import java.util.Map;

@Slf4j
@SpringBootTest
class RedisTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void test() throws Exception {
        // 保存字符串
        stringRedisTemplate.opsForValue().set("user", "bing");
    }

    @Test
    void test3() {
        // 当key: rank 不存在时，先初始化为0 再++
        Long rank = stringRedisTemplate.opsForValue().increment("rank");
        System.out.println(rank);
    }

    @Test
    void test4() {
        Long rank = orderService.getPaidCount();
        System.out.println(rank);
    }

    @Test
    void test5() {
        Long rank = orderService.incrPaidCount();
        System.out.println(rank);
    }

    @Test
    void test6() {
        Long rank = orderService.getRank("oKl-L5Q6a552IhMVzr363PO2EULQ");
        System.out.println(rank);
    }

    @Test
    void test7() {
        orderService.trySetRank("oKl-L5Q6a552IhMVzr363PO2EULQ", 2L);
    }

    @Test
    void test8() {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map<String, String> userEntries = hashOperations.entries(CacheKeyConst.bak_user_list_key);
        log.info(JSON.toJSONString(userEntries));

        hashOperations.delete(CacheKeyConst.bak_user_list_key, userEntries.keySet().toArray());
    }

    @Test
    void test9() {
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        setOperations.add(CacheKeyConst.user_paid_set_key, "oXxpqwHdDrBuLmM0e9SU5afT4W6E");
    }

    @Test
    void test10() {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String userOrderCount = hashOperations.get(CacheKeyConst.user_order_list_key, "ovE2Av_jBeLfh_XdDzjGlLbHDYk4");
        Integer count = 0;
        if (!StringUtils.isEmpty(userOrderCount)) {
            count = Integer.parseInt(userOrderCount);
        }
        hashOperations.put(CacheKeyConst.user_order_list_key, "ovE2Av_jBeLfh_XdDzjGlLbHDYk4", String.valueOf(++count));
    }
}
