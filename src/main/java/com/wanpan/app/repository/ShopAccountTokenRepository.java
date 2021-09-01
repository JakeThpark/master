package com.wanpan.app.repository;

import com.wanpan.app.entity.ShopAccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopAccountTokenRepository extends JpaRepository<ShopAccountToken, Long>{
    Optional<ShopAccountToken> findByShopIdAndAccountIdAndType(String shopId, String accountId, ShopAccountToken.Type tokenType);
}