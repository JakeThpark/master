package com.wanpan.app.repository;

import com.wanpan.app.entity.TestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {
    int countByName(String name);
    TestCategory findByNameAndParentId(String name, Long parentId);
    List<TestCategory> findByParentId(Long parentId);
}
