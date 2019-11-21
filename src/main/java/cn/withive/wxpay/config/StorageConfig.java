package cn.withive.wxpay.config;

import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

@Component
@Slf4j
public class StorageConfig {

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public StorageConfig(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

        Boolean hasKey = stringRedisTemplate.hasKey(CacheKeyConst.storage_strategy_key);
        if (!hasKey) {
            setStrategy(StorageStrategyEnum.database);
        }
    }

    public void setStrategy (StorageStrategyEnum storageStrategy) {
        // 使用枚举名存储
        stringRedisTemplate.opsForValue()
                .set(CacheKeyConst.storage_strategy_key, storageStrategy.name());
    }

    /**
     * 获取数据库存储策略，默认为database策略
     * database：数据直接存储到数据库
     * redis：数据将先存储在redis，后在数据同步任务中将数据存储到数据库。同步任务详见 DataSyncJob.class
     * @return 不存在此配置时将默认返回 database 策略
     */
    public @NonNull StorageStrategyEnum getStrategy() {
        String str = stringRedisTemplate.opsForValue().get(CacheKeyConst.storage_strategy_key);

        if (StringUtils.isEmpty(str)) {
            setStrategy(StorageStrategyEnum.database);
        }

        try {
            StorageStrategyEnum storageStrategy = StorageStrategyEnum.valueOf(str);
            return storageStrategy;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());

            return StorageStrategyEnum.database;
        }
    }
}
