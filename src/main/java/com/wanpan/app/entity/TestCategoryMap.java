package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity(name="TestCategoryMap")
@Table(name="category_map")
public class TestCategoryMap extends BaseEntity {
    private long categoryId;
    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;
//    private long shopId;
    private long shopCategoryId;
    private String shopCategoryCode;
}
