package com.wanpan.app.dto.job;

import com.wanpan.app.dto.reebonz.DetailImageRequest;
import com.wanpan.app.dto.reebonz.ReebonzStock;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/*
 * Job에서 여러 쇼핑몰의 등록정보를 한번에 받아올 수 있어야 한다.
 */
@Data
@NoArgsConstructor
public class RegisterProductRequest {
    private String createdFrom;
    private String name;
    private String code;
    private long brandId;
    private int marketplacePrice;
    private Double commission;
    private String material;
    private String color;
    private String modelName;
    private String season;
    private String productFeature;
    private String sizeStandard;
    private String description;
    private String legalInfo;
    private String productNotification;
    private String productTip;
    private String sizeInfo;
    private long categoryGenderId;
    private long categoryMasterId;
    private long categorySlaveId;
    private String imageMainUrl;
    private String imageMainOverUrl;
    private List<DetailImageRequest> detailImages;
    private List<ReebonzStock> stocks;
}
