package cn.withive.wxpay.repository;

import cn.withive.wxpay.entity.Order;
import cn.withive.wxpay.entity.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Transient;
import java.util.List;
import java.util.Optional;

public interface WechatUserRepository extends JpaRepository<WechatUser, String> {

    WechatUser findByOpenId(String openId);

    boolean existsByOpenId(String openId);

    @Override
    @Transactional
    <S extends WechatUser> List<S> saveAll(Iterable<S> entities);
}
