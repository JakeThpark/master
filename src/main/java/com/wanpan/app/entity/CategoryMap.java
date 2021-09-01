package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity(name="CategoryMap")
@Table(name="category_map")
public class CategoryMap extends BaseEntity {
    private long categoryId;
    @Column(name = "shop_id")
    private String shopId;
    private long shopCategoryId;
    private String shopCategoryCode;
}
