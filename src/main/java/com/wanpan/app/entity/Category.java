package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
public class Category extends BaseEntity {
    private String name;
    private long notificationTypeId;
    private Long parentId;
}
