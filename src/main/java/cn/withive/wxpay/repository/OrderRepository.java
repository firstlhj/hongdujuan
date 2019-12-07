package cn.withive.wxpay.repository;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

//    Order findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status);

    Order findByCode(String code);

    boolean existsByWechatOpenIdAndStatus(String openId, OrderStatusEnum status);

    @Override
    @Transactional
    <S extends Order> List<S> saveAll(Iterable<S> entities);
    
    List<Order> findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status);
}
