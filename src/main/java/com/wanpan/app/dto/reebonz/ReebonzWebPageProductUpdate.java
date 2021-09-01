package com.wanpan.app.dto.reebonz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReebonzWebPageProductUpdate {

    /*
     * 상품 수정 시에 업데이트 대상 상품 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoUpdateTarget {
        List<ProductImageClearTarget> productImageClearTargetList = new ArrayList<>();
        List<ReebonzProductOptionStock> productOptionUpdateTargetList = new ArrayList<>();
    }


    /*
     * 상품 수정 시에 초기화 대상 상품 이미지 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageClearTarget {
        String id;
        String type; // 이미지 타입 - "img_representative", "img_details"
    }

}