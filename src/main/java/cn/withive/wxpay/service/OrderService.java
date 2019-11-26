package cn.withive.wxpay.service;

import cn.withive.wxpay.config.StorageConfig;
import cn.withive.wxpay.constant.CacheKeyConst;
import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.repository.OrderRepository;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired
    private WXService WXService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StorageConfig storageConfig;

    /**
     * 获取已支付订单总数
     *
     * @return
     */
    public Long getPaidCount() {
        String str = stringRedisTemplate.opsForValue().get(CacheKeyConst.order_count_key);
        Long result = 0L;
        if (str != null) {
            result = Long.valueOf(str);
        }

        return result;
    }

    /**
     * 自增已支付订单总数
     *
     * @return
     */
    public Long incrPaidCount() {
        Long rank = stringRedisTemplate.opsForValue().increment(CacheKeyConst.order_count_key);
        return rank;
    }

    /**
     * 获取用户排名
     *
     * @param openId
     * @return
     */
    public Long getRank(String openId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        String value = hashOperations.get(CacheKeyConst.order_rank_key, openId);

        Long result = 0L;
        if (value != null) {
            result = Long.valueOf(value);
        }

        return result;
    }

    /**
     * 尝试设置用户排名
     * 当，当前排名落后已有排名时，设置失败 返回false
     *
     * @param openId
     * @param value
     */
    public boolean trySetRank(String openId, Long value) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        Long old = this.getRank(openId);
        if (old != 0 && value >= old) {
            return false;
        }

        hashOperations.put(CacheKeyConst.order_rank_key, openId, Long.toString(value));
        return true;
    }

    /**
     * 判断订单是否超时
     * 对超时订单将<b>更新</b>为 已取消
     *
     * @param order
     * @return true: 超时，false: 未超时
     */
    public boolean checkOvertime(@NonNull Order order) {
        // 有订单，判断是否超时
        LocalDateTime createTime = order.getCreatTime();

        if (LocalDateTime.now().isAfter(createTime.plusHours(2))) {
            // 超时订单，标记为超时
            this.markToCancel(order);

            return true;
        } else {

            return false;
        }
    }

    /**
     * 根据订单号检查是否为已支付订单
     * 判断是否支付，使用微信订单查询接口进行验证
     *
     * @param code
     * @return
     */
    public boolean checkPaidWithCode(String code) {
        Map<String, String> orderInfo = WXService.orderQueryByCode(code);

        if (orderInfo == null) {
            // 微信订单不存在
            return false;
        }

        boolean isPaid = "SUCCESS".equals(orderInfo.get("return_code")) &&
                "SUCCESS".equals(orderInfo.get("result_code")) &&
                "SUCCESS".equals(orderInfo.get("trade_state"));

        return isPaid;
    }

    public @Nullable
    Order findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        String str = hashOperations.get(CacheKeyConst.order_list_key, openId);

        Order result = null;
        if (StringUtils.isEmpty(str)) {
            // 缓存中不存在，去数据库中查
            result = orderRepository.findByWechatOpenIdAndStatus(openId, status);
        } else {
            // 缓存中存在，反序列化
            Order entity = JSON.parseObject(str, Order.class);
            if (entity.getStatus() == status) {
                result = entity;
            }
        }

        return result;
    }

    /**
     * 创建订单
     *
     * @param order
     * @return
     */
    public Order create(@NonNull Order order) {
        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();

        order.setId(RandomUtil.generateUniqueStr());
        order.setCreatTime(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.Created);

        switch (storageStrategy) {
            case database:
                orderRepository.save(order);
                break;
            case redis:
                HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
                hashOperations.put(CacheKeyConst.order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                hashOperations.put(CacheKeyConst.bak_order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                break;
        }

        return order;
    }

    /**
     * 根据订单实体查找订单实体
     * @param order
     * @return
     */
    public Order find(Order order) {
        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
        Order entity = null;
        switch (storageStrategy) {
            case database:
                entity = orderRepository.findById(order.getId())
                        .orElseThrow(()-> new EntityNotFoundException("订单实体未找到"));
                break;
            case redis:
                HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
                String str = hashOperations.get(CacheKeyConst.order_list_key, order.getWechatOpenId());
                if (!StringUtils.isEmpty(str)) {
                    entity = JSON.parseObject(str, Order.class);
                }
                break;
        }
        return entity;
    }

    /**
     * 将订单标记为已支付
     *
     * @param order
     * @return false : 数据库中不存在该订单 true: 标记成功
     */
    public boolean markToPaid(@NonNull Order order) {

        synchronized (order.getId()) {

            Order entity = this.find(order);

            if (entity == null) {
                throw new EntityNotFoundException("订单不存在");
            }

            if (!entity.getStatus().equals(OrderStatusEnum.Created)) {
                // 订单状态已经被修改
                return false;
            }

            order.setStatus(OrderStatusEnum.Paid);
            order.setPayTime(LocalDateTime.now());

            StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
            switch (storageStrategy) {
                case database:
                    orderRepository.save(order);
                    break;
                case redis:
                    HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
                    hashOperations.put(CacheKeyConst.order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                    hashOperations.put(CacheKeyConst.bak_order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                    break;
            }

            // 当前订单总数即为自身排名
            Long rank = this.incrPaidCount();
            this.trySetRank(order.getWechatOpenId(), rank);

            // 将订单添加至 top 列表，长度限制为10
            this.addTopList(order, 10L);

            // 删除预支付id
            this.removePrepayId(order.getWechatOpenId());

            // 记录已支付用户，已去重
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            setOperations.add(CacheKeyConst.user_paid_set_key, order.getWechatOpenId());

            return true;
        }
    }

    /**
     * 将订单标记为已取消
     *
     * @param order
     * @return
     */
    public Order markToCancel(@NonNull Order order) {
        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();

        order.setStatus(OrderStatusEnum.Canceled);
        order.setRemark("订单支付超时");

        switch (storageStrategy) {
            case database:
                orderRepository.save(order);
                break;
            case redis:
                HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
                hashOperations.put(CacheKeyConst.order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                hashOperations.put(CacheKeyConst.bak_order_list_key, order.getWechatOpenId(), JSON.toJSONString(order));
                break;
        }

        return order;
    }

    public boolean existsByWechatOpenIdAndStatus(String openId, OrderStatusEnum status) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        boolean result;
        String str = hashOperations.get(CacheKeyConst.order_list_key, openId);

        if (StringUtils.isEmpty(str)) {
            result = orderRepository.existsByWechatOpenIdAndStatus(openId, status);
        } else {
            Order entity = JSON.parseObject(str, Order.class);

            result = entity.getStatus() == status;

            if (!result) {
                result = orderRepository.existsByWechatOpenIdAndStatus(openId, status);
            }
        }

        return result;
    }

    /**
     * 将订单添加至 top 列表中
     *
     * @param order
     * @param limit top列表长度限制
     */
    public void addTopList(Order order, Long limit) {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();

        Long size = listOperations.leftPush(CacheKeyConst.order_top_key, JSON.toJSONString(order));

        if (size != null && size >= limit) {
            listOperations.trim(CacheKeyConst.order_top_key, 0, limit - 1);
        }
    }

    /**
     * 获取用户预支付id
     *
     * @param openId
     * @return
     */
    public String getPrepayId(String openId) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        String key = CacheKeyConst.wx_prepay_key + openId;
        String prepayId = valueOperations.get(key);

        return prepayId;
    }

    /**
     * 缓存用户预支付id
     *
     * @param openId
     * @param prepayId
     */
    public void setPrepayId(String openId, String prepayId) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        String key = CacheKeyConst.wx_prepay_key + openId;

        // timeout: 两小时
        valueOperations.set(key, prepayId, 7200, TimeUnit.SECONDS);
    }

    /**
     * 移除用户预支付id
     *
     * @param openId
     * @return
     */
    public boolean removePrepayId(String openId) {
        String key = CacheKeyConst.wx_prepay_key + openId;
        Boolean result = stringRedisTemplate.delete(key);
        return result;
    }
}

