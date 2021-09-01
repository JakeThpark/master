package com.wanpan.app.dto.reebonz;

import lombok.Data;

import java.util.List;

@Data
public class ProductRequest {
    private String createdFrom;
    private String name;//
    private String code;//
    private long brandId; //
    private int marketplacePrice;
    private Double commission;
    private String material;
    private String color;//
    private String modelName;//
    private String season;//
    private String productFeature;//
    private String sizeStandard;//
    private String description;
    private String legalInfo;
    private String productNotification;
    private String productTip;
    private String sizeInfo;
    private long categoryGenderId;
    private long categoryMasterId;
    private long categorySlaveId;
    private String imageMainUrl; //
    private String imageMainOverUrl; //
    private List<DetailImageRequest> detailImages; //
    private List<ReebonzStock> stocks; //
}
