package cn.withive.wxpay.service;

import cn.withive.wxpay.config.StorageConfig;
import cn.withive.wxpay.constant.CacheKeyConstEnum;
import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.StorageStrategyEnum;
import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.repository.OrderRepository;
import cn.withive.wxpay.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private Long keyExpire = 3600L;

    @Value("${order.create_frequency_value}")
    private Long createFrequencyValue;

    @Value("${order.create_frequency_expire}")
    private Long createFrequencyExpire;

    public Page<Order> findByWechatOpenIdAndPaid(String openId, Pageable pageable) {

        Page<Order> page = null;
        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
        switch (storageStrategy) {
            case database:
                page = orderRepository.findByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid, pageable);
                break;
            case redis:
                page = new PageImpl<>(new LinkedList<>());

                ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
                Long total = zSetOperations.size(CacheKeyConstEnum.user_order_paid_key.getKey(openId));

                if (total == null) {
                    return page;
                }

                long totalPages = total % pageable.getPageSize() > 0 ? total / pageable.getPageSize() + 1 :
                        total / pageable.getPageSize();

                if (pageable.getPageNumber() >= totalPages) {
                    return page;
                }

                int start = pageable.getPageNumber() * pageable.getPageSize();
                int end = start + pageable.getPageSize() - 1;

                int lastIndex = (int) (total - 1);
                if (end > lastIndex) {
                    end = lastIndex;
                }

                // 查询订单号
                Set<String> orderCodes =
                        zSetOperations.range(CacheKeyConstEnum.user_order_paid_key.getKey(openId), start, end);
                if (orderCodes == null) {
                    return page;
                }

                List<String> keys = new LinkedList<>();
                for (String code : orderCodes) {
                    keys.add(CacheKeyConstEnum.order_list_key.getKey(code));
                }

                // 查询订单实体
                ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
                List<String> values = valueOperations.multiGet(keys);

                if (values == null) {
                    return page;
                }

                // 反序列化实体
                List<Order> entity = new LinkedList<>();
                for (String value : values) {
                    Order order = JSON.parseObject(value, Order.class);
                    entity.add(order);
                }

                page = new PageImpl<>(entity, pageable, total);

                break;
        }

        return page;
    }

    /**
     * 获取已支付订单总数
     *
     * @return
     */
    public Long getPaidCount() {
        String str = stringRedisTemplate.opsForValue().get(CacheKeyConstEnum.count_paid_key);
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
        Long rank = stringRedisTemplate.opsForValue().increment(CacheKeyConstEnum.count_paid_key.getKey());
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

        String value = hashOperations.get(CacheKeyConstEnum.user_rank_key.getKey(), openId);

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

        hashOperations.put(CacheKeyConstEnum.user_rank_key.getKey(), openId, Long.toString(value));
        return true;
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

    /**
     * 检查用户创建订单频率是否到达阈值
     *
     * @param openId
     * @return true: 大于或等于阈值 false: 小于阈值
     */
    public boolean checkUserCreateCount(String openId) {
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        Long size = operations.size(CacheKeyConstEnum.user_order_created_key.getKey(openId));
        if (size == null) {
            size = 0L;
        }
        return size >= createFrequencyValue;
    }

    /**
     * 记录用户创建订单的编号
     *
     * @param openId
     * @return
     */
    public void setUserCreateOrderCode(String openId, String orderCode) {
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();

        operations.add(CacheKeyConstEnum.user_order_created_key.getKey(openId), orderCode, System.currentTimeMillis());

        stringRedisTemplate.expire(CacheKeyConstEnum.user_order_created_key.getKey(openId), createFrequencyExpire,
                TimeUnit.SECONDS);
    }

    /**
     * 重置用户已下单数量
     *
     * @param openId
     */
    public void resetUserCreateCount(String openId) {
        stringRedisTemplate.delete(CacheKeyConstEnum.user_order_created_key.getKey(openId));
    }


    public @Nullable
    Order findByWechatOpenIdAndCodeAndStatusIsCreated(String openId, String code) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String str = valueOperations.get(CacheKeyConstEnum.order_list_key.getKey(code));

        Order result = null;
        if (StringUtils.isEmpty(str)) {
            // 缓存中不存在，去数据库中查
            result = orderRepository.findByWechatOpenIdAndCodeAndStatusIsCreated(openId, code);
        } else {
            // 缓存中存在，反序列化
            Order entity = JSON.parseObject(str, Order.class);
            if (entity.getWechatOpenId().equals(openId) && entity.getStatus() == OrderStatusEnum.Created) {
                result = entity;
            }
        }

        return result;
    }

    public @Nullable
    Order findByWechatOpenIdAndCode(String openId, String code) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String str = valueOperations.get(CacheKeyConstEnum.order_list_key.getKey(code));

        Order result = null;
        if (StringUtils.isEmpty(str)) {
            // 缓存中不存在，去数据库中查
            result = orderRepository.findByWechatOpenIdAndCode(openId, code);
        } else {
            // 缓存中存在，反序列化
            Order entity = JSON.parseObject(str, Order.class);
            if (entity.getWechatOpenId().equals(openId)) {
                result = entity;
            }
        }

        return result;
    }

//    public @Nullable
//    Order findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status) {
//        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
//        String str = hashOperations.get(CacheKeyConst.order_list_key, openId);
//
//        Order result = null;
//        if (StringUtils.isEmpty(str)) {
//            // 缓存中不存在，去数据库中查
//            result = orderRepository.findByWechatOpenIdAndStatus(openId, status);
//        } else {
//            // 缓存中存在，反序列化
//            Order entity = JSON.parseObject(str, Order.class);
//            if (entity.getStatus() == status) {
//                result = entity;
//            }
//        }
//
//        return result;
//    }

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
                ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
                valueOperations.set(CacheKeyConstEnum.order_list_key.getKey(order.getCode()),
                        JSON.toJSONString(order));
                valueOperations.set(CacheKeyConstEnum.bak_order_list_key.getKey(order.getCode()),
                        JSON.toJSONString(order));

                stringRedisTemplate.expire(CacheKeyConstEnum.order_list_key.getKey(order.getCode()), keyExpire,
                        TimeUnit.SECONDS);
                break;
        }

        return order;
    }

    /**
     * 根据订单实体查找订单实体
     *
     * @param order
     * @return
     */
    public Order find(Order order) {
        StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
        Order entity = null;
        switch (storageStrategy) {
            case database:
                entity = orderRepository.findById(order.getId())
                        .orElseThrow(() -> new EntityNotFoundException("订单实体未找到"));
                break;
            case redis:
                ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
                String str = valueOperations.get(CacheKeyConstEnum.order_list_key.getKey(order.getCode()));
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
     * @return false: 标记失败 true: 标记成功
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

            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

            StorageStrategyEnum storageStrategy = storageConfig.getStrategy();
            switch (storageStrategy) {
                case database:
                    orderRepository.save(order);
                    break;
                case redis:
                    valueOperations.set(CacheKeyConstEnum.order_list_key.getKey(order.getCode()),
                            JSON.toJSONString(order));
                    valueOperations.set(CacheKeyConstEnum.bak_order_list_key.getKey(order.getCode()),
                            JSON.toJSONString(order));

                    stringRedisTemplate.persist(CacheKeyConstEnum.order_list_key.getKey(order.getCode()));
                    break;
            }

            // 记录用户已支付的订单
            ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
            zSetOperations.add(CacheKeyConstEnum.user_order_paid_key.getKey(order.getWechatOpenId()), order.getCode()
                    , System.currentTimeMillis());

            // 统计用户排名
            Long rank = this.incrPaidCount();
            this.trySetRank(order.getWechatOpenId(), rank);

            // 统计总种植棵数
            valueOperations.increment(CacheKeyConstEnum.count_tree_key.getKey(), order.getQuantity());

            // 统计用户种植棵数
            HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
            hashOperations.increment(CacheKeyConstEnum.user_tree_key.getKey(), order.getWechatOpenId(),
                    order.getQuantity());

            // 将订单添加至 top 列表，不限长度
            this.addTopList(order, 0L);

            // 删除支付参数
            this.removePayParams(order.getCode());

            // 重置用户的创建订单列表
            this.resetUserCreateCount(order.getWechatOpenId());

            return true;
        }
    }

    public boolean existsByWechatOpenIdAndPaid(String openId) {
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();

        Long size = operations.size(CacheKeyConstEnum.user_order_paid_key.getKey(openId));

        if (size == null || size == 0L) {
            // 缓存中不存在，那么去数据库中验证
            return orderRepository.existsByWechatOpenIdAndStatus(openId, OrderStatusEnum.Paid);
        } else {
            return true;
        }
    }

    /**
     * 将订单添加至 top 列表中
     *
     * @param order
     * @param limit top列表长度限制，为0表示不限制
     */
    public void addTopList(Order order, Long limit) {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();

        Long size = listOperations.leftPush(CacheKeyConstEnum.order_top_key.getKey(), JSON.toJSONString(order));
        listOperations.leftPush(CacheKeyConstEnum.order_secondtop_key.getKey(), JSON.toJSONString(order));

        if (limit != 0L && size != null && size >= limit) {
            listOperations.trim(CacheKeyConstEnum.order_top_key.getKey(), 0, limit - 1);
        }
    }

    public void setPayParamsCache(String orderCode, String payParams) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        String key = CacheKeyConstEnum.user_pay_params_key.getKey(orderCode);

        // timeout: 两小时
        valueOperations.set(key, payParams, keyExpire, TimeUnit.SECONDS);
    }

    public String getPayParams(String orderCode) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        String key = CacheKeyConstEnum.user_pay_params_key.getKey(orderCode);
        String payParams = valueOperations.get(key);

        return payParams;
    }

    public boolean removePayParams(String orderCode) {
        String key = CacheKeyConstEnum.user_pay_params_key.getKey(orderCode);
        Boolean result = stringRedisTemplate.delete(key);
        return result;
    }
}

