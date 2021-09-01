package com.wanpan.app.dto.mustit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MustitSale {

    //////// 판매글 아이디 정보 //////////

    private String saleId;

    //////// 카테고리/브랜드 선택 ////////

    /** brand
     * 브랜드 코드
     */
    private String brandCode;

    /** brandnmh
     * 브랜드 명칭
     */
    private String brandName;

    /** category_flag
     * 성별 카테고리 코드
     * 형식: Women(W), Men(M), Kids(K), Life(L)
     */
    private String genderCategoryCode;

    /** category
     * 대분류 카테고리 코드
     * 형식: 17
     */
    private String largeCategoryCode;

    /** company
     * 중분류 카테고리 코드
     * 형식: 17r06
     */
    private String mediumCategoryCode;

    /** type
     * 소분류 카테고리 코드
     * 형식: 17r06r02
     */
    private String smallCategoryCode;

    /** filters
     * 필터값 목록
     * 형식: 323,324,325,326,327,328
     */
    private String filters;

    /** filter_yn
     * 필터값 존재 여부
     * 형식: 없으면 0, 있으면 1
     */
    private String filterExistenceFlag;

    //////// 상품명 입력 ////////

    /** product_name
     * 상품명
     */
    private String productName;


    /** memo
     * 한줄메모
     * 고정값: ""
     */
    final String memo = "";

    ////////  상품정보 ////////

    /** 옵션별 배송정보 적용 여부
     * option_delivery
     * 형식: *미적용(no), 적용(ok)
     * 고정값: no
     */
    private final String optionDelivery = "no";

    /**
     * 색상-사이즈별 판매수량 목록
     */
    private final List<MustitProductOptionStock> optionStockList = new ArrayList<>();

    /** product_sangtae
     * 상품상태
     * 형식: 새제품(0), 중고제품(1)
     */
    private String productCondition;

    /** wonsanji_text
     * 원산지
     * 형식: 한국
     */
    private String wonsanji;

    /** bonus_option
     * 사은품정보
     * 고정값: ""
     */
    private final String bonusOption = "";

    /** hongbo
     * 이벤트정보
     * 고정값: ""
     */
    private final String event = "";

    /** sales_type
     * 판매형태
     * 형식: 병행수입(1), 구매대행(2), 공식수입(3), 해외직배송(선매입)(4)
     */
    private String salesType;

    /** kc_target
     * KC 안전인증
     * 형식: 선택(), 안전인증대상(1), 안전확인대상(2), 공급자적합성확인대상(3), 미인증대상(4)
     * 고정값: 4
     */
    private final String kcTarget = "4";

    /** sijoong_price_tmp
     * 판매가격
     * 형식: 1100000
     */
    private String sellingPrice;

    /** point_tmp
     * 즉시할인
     * 형식: 12000
     * 고정값: 0
     */
    private final String immediateDiscount = "0";

    /** cupon_price_tmp
     * 단골할인
     * 형식: 12000
     * 고정값: 0
     */
    private final String patronDiscount = "0";

    //////// 배송정보 ////////

    /** baesong_from
     * 배송방법
     * 형식: 국내배송(0), 해외배송(1), 해외직배송(2)
     */
    private String deliveryMethod;

    /** baesong_jumin
     * 통관고유부호 필요 여부
     * 형식: 불필요(0), 필요(1)
     * 고정값: 0
     */
    private final String customsClearanceCodeNeedFlag = "0";

    /** customs_duties
     * 관부가세 포함 여부
     * 형식: 미포함(N), 포함(Y)
     * 고정값: N
     */
    private final String customsDutiesInclusionFlag = "N";

    /** baesong_kind
     * 배송종류
     * 형식: 일반택배(normal), 화물직배송(direct)
     * 고정값: normal
     */
    private final String deliveryKind = "normal";

    /** baesong_type
     * 배송비 유형
     * 형식: 무료(배송비 포함)(0), 착불(상품수령 후 지불)(1), 선결제(주문결제시 합산)(2)
     */
    private String deliveryFeeType;

    /** baesongbi
     * 배송비용
     * 형식: 10000
     */
    private String deliveryFee;

    /** deliveryExceptionChoice
     * 추가배송비 적용 여부
     * 형식: 미적용(N), 적용(Y)
     * 고정값: N
     */
    private final String deliveryExceptionFlag = "N";

    /** bundle_delivery_type
     * 묶음배송 가능 여부
     * 형식: 불가(1), 가능(2)
     * 고정값: 1
     */
    private final String bundleDeliveryType = "1";

    /** deliveryFeeUseEach
     * 개별배송비 사용 여부
     * 형식: 사용안함(N), 사용함(Y)
     * 고정값: N
     */
    private final String eachDeliveryFeeUseFlag = "N";

    //////// 상품 이미지 ////////

    /**
     * 상품 이미지 목록
     */
    private final List<MustitProductImage> imageList = new ArrayList<>();

    //////// 상품 상세정보 ////////

    /** ir1
     * 상세정보
     */
    private String detail;

    /////// 유료 부가서비스 옵션 ////////

    /** premium
     * 프리미엄 등록 설정
     * 형식: 안함(0|0), 1일700원(1|700), 7일3500원(7|3500), 15일6000원(15|6000), 30일10000원(30|10000), 50일(50|15000)
     */
    private String premiumSale;

    /** bold
     * 상품명 굵게 표시
     * 형식: 안함(0|0), 1일300원(1|300), 7일1400원(7|1400), 15일2000원(15|2000), 30일3000원(30|3000), 50일5000원(50|5000)
     */
    private String boldFont;

}
