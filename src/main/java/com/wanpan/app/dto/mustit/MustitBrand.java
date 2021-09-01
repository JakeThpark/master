package com.wanpan.app.dto.mustit;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MustitBrand {
    private long brandId;
    private String brandName;

    public MustitBrand(long brandId, String brandName){
        this.brandId = brandId;
        this.brandName = brandName;
    }
}
