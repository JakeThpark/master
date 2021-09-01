package com.wanpan.app.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
public class BrandMap extends BaseEntity {
    private long brandId;
    private String shopId;
    private String sourceCode;
    private String sourceName;

    public BrandMap(long brandId, String shopId, String sourceCode, String sourceName){
        this.brandId = brandId;
        this.shopId = shopId;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
    }

}
