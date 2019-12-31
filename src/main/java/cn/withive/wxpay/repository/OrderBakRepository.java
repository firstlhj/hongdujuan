package cn.withive.wxpay.repository;

import cn.withive.wxpay.constant.OrderStatusEnum;
import cn.withive.wxpay.constant.OrderTypeEnum;
import cn.withive.wxpay.entity.OrderBak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderBakRepository extends JpaRepository<OrderBak, String> {

//    Order findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status);

    boolean existsByWechatOpenIdAndStatus(String openId, OrderStatusEnum status);

    @Override
    @Transactional
    <S extends OrderBak> List<S> saveAll(Iterable<S> entities);

    Page<OrderBak> findByWechatOpenIdAndStatus(String openId, OrderStatusEnum status, Pageable pageable);

    @Query(value = "select t from OrderBak t where t.wechatOpenId =?1 and t.type=?2 and t.status=0")
    OrderBak findByWechatOpenIdAndTypeAndStatusIsCreated(String openId, OrderTypeEnum type);

    @Query(value = "select t from OrderBak t where t.wechatOpenId =?1 and t.code =?2 and t.status=0")
    OrderBak findByWechatOpenIdAndCodeAndStatusIsCreated(String openId, String code);

    OrderBak findByWechatOpenIdAndCode(String openId, String code);

    boolean existsByWechatOpenIdAndCodeAndStatus(String openId, String code, OrderStatusEnum status);
}
