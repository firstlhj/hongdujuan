package cn.withive.wxpay.repository;

import cn.withive.wxpay.entity.WechatUserBak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WechatUserBakRepository extends JpaRepository<WechatUserBak, String> {

    WechatUserBak findByOpenId(String openId);

    boolean existsByOpenId(String openId);

    @Override
    @Transactional
    <S extends WechatUserBak> List<S> saveAll(Iterable<S> entities);
}
