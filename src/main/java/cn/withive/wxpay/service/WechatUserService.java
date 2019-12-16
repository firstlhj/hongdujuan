package cn.withive.wxpay.service;

import cn.withive.wxpay.config.StorageConfig;
import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.repository.WechatUserRepository;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

@Service
public class WechatUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private StorageConfig storageConfig;

    public WechatUser save(WechatUser wechatUser) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        // 不管什么存储策略，用户数据就是要缓存
        hashOperations.put(CacheKeyConstEnum.user_list_key.getKey(), wechatUser.getOpenId(),
                JSON.toJSONString(wechatUser));

        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();

        switch (storageStrategy) {
            case database:
                wechatUserRepository.save(wechatUser);
                break;
            case redis:
                hashOperations.put(CacheKeyConstEnum.bak_user_list_key.getKey(), wechatUser.getOpenId(),
                        JSON.toJSONString(wechatUser));
                break;
        }

        return wechatUser;
    }

    public WechatUser findByOpenId(String openId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        String str = hashOperations.get(CacheKeyConstEnum.user_list_key.getKey(), openId);

        WechatUser result;
        if (StringUtils.isEmpty(str)) {
            // 缓存中不存在，去数据库中查找
            result = wechatUserRepository.findByOpenId(openId);
        } else {
            // 缓存中存在，反序列化
            result = JSON.parseObject(str, WechatUser.class);
        }

        return result;
    }

    public boolean existsByOpenId(String openId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        Boolean isExist = hashOperations.hasKey(CacheKeyConstEnum.user_list_key.getKey(), openId);

        if (!isExist) {
            isExist = wechatUserRepository.existsByOpenId(openId);
        }

        return isExist;
    }

    public Long getOrderCount(String openId) {
        HashOperations<String, String, String> operations = stringRedisTemplate.opsForHash();
        String str = operations.get(CacheKeyConstEnum.user_tree_key.getKey(), openId);

        Long count = 0L;
        if (!StringUtils.isEmpty(str)) {
            count = Long.valueOf(str);
        }

        return count;
    }
}
