package com.wanpan.app.dto.reebonz;

import lombok.Data;

import java.util.List;

@Data
public class ProductMetaInfo {
    private String code;
    private String name;
    private long brand_id;
    private String brand_name;
    private boolean updateAvailable;
    private String color;
    private String modelName;
    private String season;
    private String productFeature;

    private List<ReebonzCategory> categories;
}
