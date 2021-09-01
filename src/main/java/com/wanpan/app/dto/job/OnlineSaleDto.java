package com.wanpan.app.dto.job;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class OnlineSaleDto {
    private String subject; //판매글 제목 "prd123"
    private long price; //기준 판매가
    private ProductImportType importType; //수입유형 "PARALLEL_IMPORT"
    private ProductCondition condition; //제품상태 "UNUSED"
    private String seasonYear; //시즌년도 "20"
    private String season; //시즌 "NA, SS, FW"
    private String detail; //공통 상세설명 "string"

    private List<OnlineSaleImageDto> saleImageList = new ArrayList<>();
    private List<ProductDto> productList = new ArrayList<>();
    private ShopSaleJobDto.Request.PostJob shopSale; //쇼핑몰 카테고리별 등록리스트
    private BrandMapDto brandMap; //(쇼핑몰 브랜드)

    //상품 고시정보 필드
    private String productionCountry; // "string"
    private String productionCompany; // "string"
    private String productionDate; // "string"
    private String size; // "string"
    private String precaution; // "string"
    private String csStaffName; // "string"
    private String csStaffPhone; // "string"
    private String qualityAssuranceStandards; // "string"
    private String productKind; // "string"
    private String material; // "string"
    private String weight; // "string"
    private boolean warrantyExistenceFlag; // "string"
    private String specification; // "string"
    private String ingredient; // "string"
    private String mfdsCheck; // "string"
    private String useByDate; // "string"
    private String howToUse; // "string"
    private String color;    // "string"

    private Boolean directPictureFlag;

    //각 쇼핑몰별 구분된 정보들
    private OnlineSaleFeelwayDto saleFeelway;
    private OnlineSaleMustitDto saleMustit;
    private OnlineSaleReebonzDto saleReebonz;
    //고르다는 추가 데이타가 없는 관계로 필요할 때 추가예정


    @Getter
    public enum ProductImportType {
        PARALLEL_IMPORT("PARALLEL_IMPORT"),
        FORMAL_IMPORT("FORMAL_IMPORT");

        private String name;
        ProductImportType(String name) {
            this.name = name;
        }
    }

    @Getter
    public enum ProductCondition {
        UNUSED("UNUSED"),
        USED("USED");

        private String name;

        ProductCondition(String name) {
            this.name = name;
        }
    }

}
