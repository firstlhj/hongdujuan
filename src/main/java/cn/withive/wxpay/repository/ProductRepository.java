package cn.withive.wxpay.repository;

import cn.withive.wxpay.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    Product findByCode(String code);

    @Override
    @Transactional
    <S extends Product> List<S> saveAll(Iterable<S> entities);
}
