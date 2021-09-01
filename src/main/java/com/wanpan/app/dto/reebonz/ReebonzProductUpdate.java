package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ReebonzProductUpdate {
    private String createdFrom; //API 호출 아이디
    private long marketplacePrice;//Int 상품 마켓 가격(필수)
    private boolean available; //Int 판매 오픈 여부 0(미노출), 1(노출)
    private String imageMainUrl; //메인 이미지(필수)
    private String imageMainOverUrl; //오버 메인 이미지
    private String description; //상품 상세 설명 - 상품 메타는 수정 안되며 상품만 수정

    /**
     * name 수정 할 경우 name, marketplace_name 둘 다 수정됨
     */
    @JsonProperty("marketplace_name")
    private String name;//상품명(마켓용 상품명)

    @JsonProperty("marketplace_code")
    private String marketplaceCode;//판매자 Sku 정보, 코드명(마켓용 코드명)

    private Long brandId; //브랜드 ID - Brands API 참고 - 기본값 null을 위해 obj로 구성

    private Double commission; //Float 수수료(0초과~1미만) ex:0.8 은 20% float

    /**
     * 브랜드 아이디와 카테고리 아이디 리스트는 상품 메타 정보로 파트너가 생성한 메타만
     * 수정 가능합니다. is_self_created_by => true 인 상품
     * Parameter Ex) {"brand_id":299,"category_ids":[3,10,197] => 4자리수의 경우 주게 되면 처리된다.}
     */
    @JsonProperty("category_ids")
    private List<Long> categoryIds;

    private List<ReebonzDetailImage> detailImages; //상품 상세 이미지(N개)

    private String color; //색상
    private String modelName; //기타상품정보
    private String season;//시즌정보
    private String productFeature;//현재 웹사이트에서 사용되지 않음
    /**
     * 한국:kr, 이탈리아:it, 프랑스:fr, 미국:us, 영국:uk, 독일:dk, 일본:jp, 기타:etc
     */
    private String sizeStandard; //사이즈 기준 국가

}
