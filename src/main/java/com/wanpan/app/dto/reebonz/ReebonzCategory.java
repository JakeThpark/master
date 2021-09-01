package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ReebonzCategory {

    @JsonProperty("category_id")
    private long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("name_for_hangle")
    private String nameForHangle;

    @JsonProperty("sub_categories")
    private List<ReebonzCategory> subCategories;
}
