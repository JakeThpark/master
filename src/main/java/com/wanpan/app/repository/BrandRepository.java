package com.wanpan.app.repository;

import com.wanpan.app.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    int countByName(String name);
    Brand findByName(String name);
}
