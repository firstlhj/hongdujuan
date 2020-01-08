package cn.withive.wxpay.service;

import cn.withive.wxpay.config.ProjectConfig;
import cn.withive.wxpay.config.StorageConfig;
import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import cn.withive.wxpay.entity.WechatUser;
import cn.withive.wxpay.exception.TokenExpireException;
import cn.withive.wxpay.exception.WxException;
import cn.withive.wxpay.model.WXUserInfoModel;
import cn.withive.wxpay.repository.WechatUserRepository;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import jdk.nashorn.internal.runtime.regexp.joni.exception.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class WechatUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private WechatUserRepository wechatUserRepository;

    @Autowired
    private StorageConfig storageConfig;

    @Autowired
    private WXService wxService;

    @Autowired
    private ProjectConfig projectConfig;

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

    /**
     * 同步微信用户信息
     * @param openId
     * @return
     * @throws WxException
     * @throws TokenExpireException
     */
    public WechatUser syncInfo(String openId) throws WxException, TokenExpireException {
        WechatUser user = this.findByOpenId(openId);
        if (user == null) {
            WXUserInfoModel userInfoModel = wxService.getUserInfo(openId);

            user = new WechatUser();
            user.setId(RandomUtil.generateUniqueStr());
            user.setCreatTime(LocalDateTime.now());
            user.setOpenId(openId);
            user.setNickname(userInfoModel.getNickname());
            user.setAvatar(userInfoModel.getHeadimgurl());
            user.setCountry(userInfoModel.getCountry());
            user.setProvince(userInfoModel.getProvince());
            user.setCity(userInfoModel.getCity());
            this.save(user);
        } else {
            // 用户信息超过七天,或者缺少详细信息,那么拉取更新用户最新个人信息
            if (user.getCreatTime().plusDays(projectConfig.getSyncUserInfoDay()).isBefore(LocalDateTime.now()) ||
                    StringUtils.isEmptyOrWhitespace(user.getNickname())) {

                WXUserInfoModel userInfoModel = wxService.getUserInfo(openId);

                user.setCreatTime(LocalDateTime.now());
                user.setNickname(userInfoModel.getNickname());
                user.setAvatar(userInfoModel.getHeadimgurl());
                user.setCountry(userInfoModel.getCountry());
                user.setProvince(userInfoModel.getProvince());
                user.setCity(userInfoModel.getCity());
                this.save(user);
            }
        }

        return user;
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
