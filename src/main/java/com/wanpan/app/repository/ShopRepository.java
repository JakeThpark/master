package com.wanpan.app.repository;

import com.wanpan.app.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Shop findById(String id);
}
