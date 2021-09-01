package com.wanpan.app.repository;

import com.wanpan.app.entity.BrandMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandMapRepository extends JpaRepository<BrandMap, Long> {
    BrandMap findByBrandIdAndShopId(long brandId, String shopId);
}
