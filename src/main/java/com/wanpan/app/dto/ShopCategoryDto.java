package com.wanpan.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ShopCategoryDto {
    @JsonProperty("name")
    private String name;
    @JsonProperty("notificationType")
    private String notificationType;
    @JsonProperty("child")
    private List<ShopCategoryDto> child = new ArrayList<>();

}
