package cn.withive.wxpay.service;

import cn.withive.wxpay.config.StorageConfig;
import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StorageConfig storageConfig;

    public Product findByCode(String code) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String str = hashOperations.get(CacheKeyConst.product_list_key, code);

        if (!StringUtils.isEmpty(str)) {
            Product result = JSON.parseObject(str, Product.class);
            return result;
        }

        // 缓存中不存在，查找数据库
        Product result = productRepository.findByCode(code);

        if (result == null && code.equals("001")) {
            // 数据库中仍不存在，初始化数据
            result = new Product();
            result.setId(RandomUtil.generateUniqueStr());
            result.setCreatTime(LocalDateTime.now());
            result.setName("小树");
            result.setCode("001");
            result.setAmount(new BigDecimal(0.01).setScale(2, RoundingMode.HALF_UP));
            // 不管什么存储策略，产品数据就是要缓存
            hashOperations.put(CacheKeyConst.product_list_key, code, JSON.toJSONString(result));

            StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
            switch (storageStrategy) {
                case database:
                    productRepository.save(result);
                    break;
                case redis:
                    hashOperations.put(CacheKeyConst.bak_product_list_key, code, JSON.toJSONString(result));
                    break;
            }
        }

        return result;
    }
}