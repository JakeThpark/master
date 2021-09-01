package com.wanpan.app.dto.reebonz;

import lombok.Data;

import java.util.List;

@Data
public class ReebonzApiProductCreate {
    private String createdFrom; //API 호출 아이디 (필수)
    private String name;//상품명(필수)
    private String code;//Official Sku 정보(필수)
    private long brandId; //브랜드 ID - Brands API 참고(필수)
    private long marketplacePrice;//상품 마켓 가격(필수)

    private Double commission; //수수료(0초과~1미만) ex:0.8 은 20%
    private String material; //상품 재질
    private String description; //상품 상세 설명 - Templates API 선택 가능(템플릿 이름)

    private String legalInfo; //법정 카테고리 - Templates API 선택 가능(템플릿 이름)
    private String productNotification; //상품 품목 고시 정보
    private String productTip; //취급 유의 사항 - Templates API 선택 가능(템플릿 이름)
    private String sizeInfo; //사이즈 정보 - Templates API 선택 가능(템플릿 이름)
    private long categoryGenderId; //카테고리 - Categories API 참고(남성 = 2, 여성 = 3)
    private long categoryMasterId; //카테고리 - Categories API 참고
    private long categorySlaveId; //카테고리 - Categories API 참고
    private String imageMainUrl; //메인 이미지(필수)
    private String imageMainOverUrl; //오버 메인 이미지
    private List<ReebonzDetailImage> detailImages; //상품 상세 이미지(N개)
    private List<ReebonzStock> stocks; //상품 재고(필수)

    //TODO: swagger에는 존재하지만 document에 존재하지 않는 항목들(값을 전달해도 등록되지 않는다)
    private String color;//
    private String modelName;//
    private String season;//시즌정보
    private String productFeature;//
    private String sizeStandard;//

    /*
     * product post의 경우 리턴되는 형태가 달라서 별도로 사용한다.
     */
    @Data
    public static class Response{
        private String result;
        private String message;
        private Long productId;
    }

}
