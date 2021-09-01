package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
public class Brand extends BaseEntity {
    private String name;

    public Brand(String name){
        this.name = name;
    }
}
