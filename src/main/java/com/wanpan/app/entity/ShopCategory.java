package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
public class ShopCategory extends BaseEntity {

    private String shopId;
    private String name;
    private String description;
    private String shopCategoryCode;
    private Long parentId;
    private long notificationTypeId;
    private String filter;

    @ManyToOne
    @JoinColumn(name = "parentId" , insertable = false, updatable = false)
    private ShopCategory parentShopCategory;

}
