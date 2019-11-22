package cn.withive.wxpay.job;

import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.repository.OrderRepository;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.repository.WechatUserRepository;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据同步服务
 * 定时将缓存中的数据持久化到数据库
 * 数据库持久化操作已添加事务，异常时将回滚
 *
 * @author qiu xiaobing
 * @date 2019/11/18 17:43
 */
@Component
@Slf4j
public class DataSyncJob {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 每半小时
//    @Scheduled(cron = "0 0/30 * * * ? ")
//@Scheduled(cron = "0 0/1 * * * ? ")
    public void work() {
        long start = System.currentTimeMillis();

        try {
            HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

            Map<String, String> orderEntries = hashOperations.entries(CacheKeyConst.bak_order_list_key);
            Map<String, String> productEntries = hashOperations.entries(CacheKeyConst.bak_product_list_key);
            Map<String, String> userEntries = hashOperations.entries(CacheKeyConst.bak_user_list_key);

            List<Order> orders = new LinkedList<>();
            for (String orderStr : orderEntries.values()) {
                Order order = JSON.parseObject(orderStr, Order.class);
                orders.add(order);
            }
            if (!orders.isEmpty()) {
                orderRepository.saveAll(orders);
                hashOperations.delete(CacheKeyConst.bak_order_list_key, orderEntries.keySet().toArray());
            }

            List<Product> products = new LinkedList<>();
            for (String productStr : productEntries.values()) {
                Product product = JSON.parseObject(productStr, Product.class);
                products.add(product);
            }
            if (!products.isEmpty()) {
                productRepository.saveAll(products);
                hashOperations.delete(CacheKeyConst.bak_product_list_key, productEntries.keySet().toArray());
            }

            List<WechatUser> users = new LinkedList<>();
            for (String userStr : userEntries.values()) {
                WechatUser user = JSON.parseObject(userStr, WechatUser.class);
                users.add(user);
            }
            if (!users.isEmpty()) {
                wechatUserRepository.saveAll(users);
                hashOperations.delete(CacheKeyConst.bak_user_list_key, userEntries.keySet().toArray());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            log.info("数据同步服务结束，耗时：" + (end - start) + "ms");
        }
    }
}
