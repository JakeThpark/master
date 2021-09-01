package com.wanpan.app.repository;

import com.wanpan.app.entity.TestShopCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestShopCategoryRepository extends JpaRepository<TestShopCategory, Long> {
    List<TestShopCategory> findByShopIdAndParentId(String shopId, Long parentId);
}
