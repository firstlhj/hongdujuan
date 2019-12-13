package cn.withive.wxpay;

import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.constant.OrderTypeEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.OrderRepository;
import cn.withive.wxpay.service.OrderService;
import cn.withive.wxpay.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

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
    void test6() {
        orderService.setUserCreateOrderCode("oXxpqwHdDrBuLmM0e9SU5afT4W6E", "123");
        boolean isVerify = orderService.checkUserCreateCount("oXxpqwHdDrBuLmM0e9SU5afT4W6E");
        System.out.println(isVerify);
        orderService.resetUserCreateCount("oXxpqwHdDrBuLmM0e9SU5afT4W6E");
    }

    @Test
    void test7() {
        String key = CacheKeyConstEnum.order_list_key.getKey(OrderTypeEnum.myself.name());
        System.out.println(key);
    }
}
