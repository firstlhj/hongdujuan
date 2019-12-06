package cn.withive.wxpay;

import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.OrderRepository;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@SpringBootTest
public class CacheTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void test1() {
        Product product = productService.findByCode("001");

        System.out.println(product.getName());
    }

    @Test
    void test2() {
        Order order = new Order();
        order.setCode("123465");
        order.setWechatOpenId("oKl-L5Q6a552IhMVzr363PO2EULQ");
        order.setAmount(new BigDecimal(0.01));

        order = orderService.create(order);

        System.out.println(order.getId());

        order = orderRepository.save(order);
        System.out.println(order.getId());
    }

    @Test
    void test3() {
        String prepayId = orderService.getPrepayId("oXxpqwPLBVseGV5BhMxsfpkAbG8Q");
        log.info(prepayId);
    }

    @Test
    void test4() {
        orderService.setPrepayId("oXxpqwPLBVseGV5BhMxsfpkAbG8Q", "7788");
    }

    @Test
    void test5() {
        orderService.removePrepayId("oXxpqwPLBVseGV5BhMxsfpkAbG8Q");
    }
}
