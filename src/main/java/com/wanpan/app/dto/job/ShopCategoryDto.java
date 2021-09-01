package com.wanpan.app.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShopCategoryDto {
    @JsonProperty("id")
    private long categoryId; // 16,
    private String shopId;
    private String name; // "의류"
    private String description;
    private String shopCategoryCode;// "해당샾의 코드값"
    private String filter;

    private ShopCategoryDto child;
}
