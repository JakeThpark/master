package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
public class BrandNotMapping extends BaseEntity {
    private String shopId;
    private String sourceCode;
    private String sourceName;

    public BrandNotMapping(String shopId, String sourceCode, String sourceName){
        this.shopId = shopId;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;

    }
}
