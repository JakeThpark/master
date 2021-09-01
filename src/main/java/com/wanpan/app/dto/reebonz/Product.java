package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class Product {

    private long id;
    private String name;//
    private String code;//
    private String marketplaceName;
    private String marketplaceCode;
    private boolean available;
    private long brandId;//
    private String brandName;
    private int supplyPrice;
    private int sellingPrice;
    private String partnerName;
    private int totalStockCount;
    private int currentStockCount;
    private int soldStockCount;
    private int deliveryBy;
//    private Filename imageMainUrl;
//    private Filename imageMainOverUrl;
    private JsonNode imageMainUrl;
    private JsonNode imageMainOverUrl;
    private boolean isSelfCreatedBy;
    private String color;//
    private String modelName;//
    private String season;//
    private String productFeature;//
    private String sizeStandard;//

    private List<ReebonzCategory> categories;
    private List<DetailImage> detailImages;
    private List<Stock> stocks;

    //Filename오브젝트로 줄때와 String로 줄때가 존재해서 String로 받은 후에 처리해서 재입력해줌
    private Filename imageMainUrlObject;
    private Filename imageMainOverUrlObject;
}
