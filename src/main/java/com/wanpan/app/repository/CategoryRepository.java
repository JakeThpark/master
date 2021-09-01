package com.wanpan.app.repository;

import com.wanpan.app.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    int countByName(String name);
    Category findByNameAndParentId(String name, Long parentId);
}
