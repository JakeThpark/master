package com.wanpan.app.dto.godra.seller.product;

import com.wanpan.app.dto.godra.type.GordaProvisionalStatus;
import com.wanpan.app.dto.godra.type.ShoppingGender;
import com.wanpan.app.dto.job.BrandMapDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GordaMappingRequestDto {
    private long shopId;
    private String requestId;
    private GordaProvisionalStatus status; //'RECEIVED','MAPPED','REGISTERED','NOT_FOUND'
    private String name; //subject("테스트 상품입니다.")
    private ShoppingGender shoppingGender; //("WOMEN")
    private String category; //shopCategoryText ("남성>의류>바지>진")
    private String designer; //고르다 브랜드 이름 (GUCCI)
    private String color; //classificationValue (RED)

    private String seasonYear; //시즌년도 "20"(셀리스트 추가데이타 - 필요없으면 제거)
    private String season; //시즌 "NA, SS, FW"
    private long price; //기준 판매가
    private String modelNumber; //officialSku (공식 시리얼 넘버 - 사용자입력이므로 틀릴수 있음)
    private String description; //detail (판매글 상세설명)

    private long productId; //고르다의 공통 상품 ID
    private long shopProductId; //고르다의 샵 상품 ID

    //상품 이미지목록
    private List<SaleImage> saleImageList = new ArrayList<>(); //이미지목록
    //사이즈 옵션
    private List<ProductSizeOption> productOptionList = new ArrayList<>();


    @Data
    public static class SaleImage {
        private String originImagePath;
        private int sequence; // 이미지 순서
        private boolean mainFlag; // false(메인이미지 여부)
    }

    @Data
    public static class ProductSizeOption {
        private String requestOptionId;
        private String value; // 블루|250, //옵션Text
        private int quantity; // 10, //재고수량
        private long sellingPrice; // 옵션별 판매가 2300000
        private long shopProductSizeOptionId;
    }
}
