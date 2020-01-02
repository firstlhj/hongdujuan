package cn.withive.wxpay.repository;

import cn.withive.wxpay.entity.Area;
import cn.withive.wxpay.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, String> {

}
