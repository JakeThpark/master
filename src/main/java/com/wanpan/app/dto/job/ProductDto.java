package com.wanpan.app.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private long productGroupId; // 0
    private String customSku; // "prd123-RED" 관리자SKU
    private String officialSku; // "prd123R" 공식시리얼넘버(사용자가 임의로 입력한값 - 틀릴수있음)
    private String classificationType; // "COLOR"
    private String classificationValue; // "RED"
    private String standardSellingPrice; // 2300000
    private String memo; // "string"
    private String detail; // "string"

    private List<ProductOptionDto> productOptionList = new ArrayList<>();
}
