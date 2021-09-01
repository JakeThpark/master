package com.wanpan.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryDto {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("parentId")
    private String parentId;
    @JsonProperty("notificationType")
    private String notificationType;
    @JsonProperty("child")
    private List<CategoryDto> child = new ArrayList<>();

}
