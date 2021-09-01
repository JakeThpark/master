package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity(name="TestCategory")
@Table(name="category")
public class TestCategory extends BaseEntity {
    private String name;
    @Where(clause = "parentId is null")
    private Long parentId;

    @ManyToOne
    @JoinColumn(name = "notificationTypeId")
    private NotificationType notificationType;

//    @ManyToOne
//    @JoinColumn(name = "parentId" , insertable = false, updatable = false)
//    private TestCategory parentTestCategory;
////    @Where(clause = "parent_article_comment_id is null")
//
//    @OneToMany(mappedBy = "parentTestCategory", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
//    private List<TestCategory> childCategory;

    @OneToMany(cascade = CascadeType.REMOVE)
    @BatchSize(size = 10)
    @JoinColumn(name = "parentId")
    private List<TestCategory> childCategory;

    @OneToMany
    @JoinColumn(name = "categoryId")
    private List<TestCategoryMap> categoryMaps;

}
