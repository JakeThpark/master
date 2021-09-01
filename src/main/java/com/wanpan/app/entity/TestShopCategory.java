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
@Entity(name="TestShopCategory")
@Table(name="shop_category")
public class TestShopCategory extends BaseEntity {
    private String shopId;
    private String name;
    private String description;
    private String shopCategoryCode;
    @Where(clause = "parentId is null")
    private Long parentId;

    @ManyToOne
    @JoinColumn(name = "notificationTypeId")
    private NotificationType notificationType;

    private String filter;

//    @ManyToOne
//    @JoinColumn(name = "parentId" , insertable = false, updatable = false)
//    private TestShopCategory parentShopCategory;

    @OneToMany(cascade = CascadeType.REMOVE)
    @BatchSize(size = 10)
    @JoinColumn(name = "parentId")
    private List<TestShopCategory> childShopCategory;

}
