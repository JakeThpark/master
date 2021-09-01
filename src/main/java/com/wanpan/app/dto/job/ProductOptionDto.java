package com.wanpan.app.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDto {
    private long id;
    private String name; // 250,
    private int quantity; // 10,
    private long sellingPrice; // 2300000
    private List<ProductOptionMapDto> productOptionMapList;
}
