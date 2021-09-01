package com.wanpan.app.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandMapDto {
//    private long brandId;
//    private long shopId;
    private String sourceCode;
    private String sourceName;
    private Boolean directFlag;
}
