package com.wanpan.app.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Entity(name="ShopCategoryForInsert")
@Table(name="shop_category")
public class ShopCategoryForInsert {
    @Id
    private long id;

    @CreatedDate
    private LocalDateTime createAt;

    @LastModifiedDate
    private LocalDateTime updateAt;

    private String shopId;
    private String name;
    private String description;
    private String shopCategoryCode;
    private Long parentId;
    private long notificationTypeId;
    private String filter;

}