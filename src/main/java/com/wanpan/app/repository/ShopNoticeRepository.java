package com.wanpan.app.repository;

import com.wanpan.app.entity.Shop;
import com.wanpan.app.entity.ShopNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopNoticeRepository extends JpaRepository<ShopNotice, Long> {
    List<ShopNotice> findByShop(Shop shop);
//    ShopNotice findByShop_TypeAndShopNoticeId(Shop.Type shopType, String shopNoticeId);
}
