package cn.withive.wxpay.service;

import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.entity.Area;
import cn.withive.wxpay.entity.Product;
import cn.withive.wxpay.repository.AreaRepository;
import cn.withive.wxpay.repository.ProductRepository;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Area findByCode(String code) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String str = hashOperations.get(CacheKeyConstEnum.area_list_key.getKey(), code);

        if (!StringUtils.isEmpty(str)) {
            Area result = JSON.parseObject(str, Area.class);
            return result;
        }

        // 缓存中不存在，查找数据库
        Area result = areaRepository.findById(code).orElseThrow(() -> new EntityNotFoundException("根据code未找到该实体"));

        hashOperations.put(CacheKeyConstEnum.area_list_key.getKey(), code, JSON.toJSONString(result));

        return result;
    }

    public boolean exist(String code) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Boolean result = hashOperations.hasKey(CacheKeyConstEnum.area_list_key.getKey(), code);

        if (!result) {
            Optional<Area> optional = areaRepository.findById(code);
            optional.ifPresent((area)-> {
                hashOperations.put(CacheKeyConstEnum.area_list_key.getKey(), code, JSON.toJSONString(area));
            });

            result = optional.isPresent();
        }

        return result;
    }
}
