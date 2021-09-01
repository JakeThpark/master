package com.wanpan.app.repository;

import com.wanpan.app.entity.ShopCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopCategoryRepository extends JpaRepository<ShopCategory, Long> {
    ShopCategory findByShopIdAndNameAndParentId(String shopId, String name, Long parentId);
    List<ShopCategory> findByShopIdAndParentId(String shopId, Long parentId);
}
