package cn.withive.wxpay;

import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.service.ProductService;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class DbTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Test
    void test1() {
        Product product = new Product();
        product.setName("小树");
        product.setCode("001");
        product.setAmount(new BigDecimal(0.01));

        productRepository.save(product);
    }

    @Test
    void test2() {
        List<Product> all = productRepository.findAll();
        log.info(all.toString());
    }

    @Test
    void test3() {
        Product product = productService.findByCode("001");
        log.info(JSON.toJSONString(product));
    }

    @Test
    void test4() throws Exception {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Product product = new Product();
            product.setId(RandomUtil.generateUniqueStr());
            product.setCreatTime(LocalDateTime.now());
            product.setName("test:" + i);

            if (i == 4) {
//                product.setId(null);
            }
            products.add(product);
        }

        productRepository.saveAll(products);
    }
}
