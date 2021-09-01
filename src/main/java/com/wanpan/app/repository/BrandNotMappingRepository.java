package com.wanpan.app.repository;

import com.wanpan.app.entity.BrandNotMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandNotMappingRepository extends JpaRepository<BrandNotMapping, Long> {
    List<BrandNotMapping> findByShopIdAndSourceCodeAndSourceName(String shopId, String sourceCode, String sourceName);
}
