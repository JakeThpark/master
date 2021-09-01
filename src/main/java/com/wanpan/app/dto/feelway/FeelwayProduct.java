package com.wanpan.app.dto.feelway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.job.OnlineSaleDto;
import lombok.Data;
import lombok.Getter;

@Data
public class FeelwayProduct {
    @JsonProperty("g_buy_price")
    String goodsBuyPrice = "";                 // 실제로는 사용되지 않는 데이터
    @JsonProperty("rd_no")
    String randomId;                           // 랜덤값, 페이지데이터
    @JsonProperty("u_id")
    String userId;                             // 유저의 로그인 ID, 페이지데이터
    @JsonProperty("mode")
    String mode = "";                          // 생성시: 데이터 없음, 수정시: modify, 복사시:copy_new
    @JsonProperty("g_no")
    String goodsId = "";                       // 수정할대 쓰이는 상품 ID 같다
    @JsonProperty("max_minus_emoney")
    String maxMinusEMoney;                     // 최소 eMoney 잔액, 페이지데이터
    @JsonProperty("g_price_back")
    String goodsPriceBack;                     // g_price 와 같게
    @JsonProperty("editor")
    String editor = "TEXT";                    // 에디터 타입
    @JsonProperty("g_price")
    String goodsPrice;                         // <판매가격-희망가격>
    @JsonProperty("g_price_kor")
    String goodsPriceKorean = "";              // <판매가격-가격확인>
    @JsonProperty("bosang_text")
    String bosangText = "2배보상하겠음";           // 위조상품 판매 방지 문자
    @JsonProperty("brand_no")
    String brandId;                             // <브랜드>
    @JsonProperty("ot_brand_name")
    String otherBrandName = "";                 // <브랜드명 직접기재>
    @JsonProperty("new_product")
    String newProduct;                          // <새상품/중고품>, 새상품(2)/중고품(1)
    @JsonProperty("cate_no")
    String categoryId;                          // <대분류>
    @JsonProperty("sub_cate_no")
    String subCategoryId;                       // <소분류>
    @JsonProperty("g_name")
    String goodsName = "";                      // <상품명>
    @JsonProperty("g_name_add_key")
    String goodsNameAddKey = "";                // <검색어추가>
    @JsonProperty("g_origin")
    String goodsOrigin = "";                    // <원산지(제조국)>
    @JsonProperty("g_size")
    String goodsSize = "";                      // <사이즈>
    @JsonProperty("g_buy_year")
    String goodsBuyYear = "";                   // <구입연도>
    @JsonProperty("g_buy_place")
    String goodsBuyPlace = "";                  // <구입장소>
    @JsonProperty("material")
    String material = "";                       // <제품소재>
    @JsonProperty("color")
    String color = "";                          // <색상>

    @JsonProperty("stock_total_amount")
    String stockTotalAmount = "1";            // <판매수량>

    @JsonProperty("company")
    String company = "";                        // <제조사/수입사>
    @JsonProperty("notice")
    String notice = "";                         // <취급(사용/세탁)시주의사항>
    @JsonProperty("standard_guarantee")
    String standardGuarantee = "";              // <품질보증기준>
    @JsonProperty("as_manager")
    String customerServiceManager = "";         // <A/S책임자>, 자동으로 입력되어 있음
    @JsonProperty("as_manager_phone")
    String customerServiceManagerPhone = "";    // <A/S책임자 - 전화번호>
    @JsonProperty("weight")
    String weight = "";                         // <중량/용량>
    @JsonProperty("essential")
    String essential = "";                      // <제품주요사양>
    @JsonProperty("date_period")
    String datePeriod = "";                     // <제조연월/사용기한>
    @JsonProperty("use_method")
    String useMethod = "";                      // <사용방법>
    @JsonProperty("ingredient")
    String ingredient = "";                     // <주요성분>
    @JsonProperty("evaluation")
    String evaluation = "";                     // <심사필유무>
    @JsonProperty("specification")
    String specification = "";                  // <주요사양>
    @JsonProperty("jewel_class")
    String jewelryClass = "";                   // <등급(귀금속/보석)>
    @JsonProperty("watch_function")
    String watchFunction = "";                  // <계기능(방수등)>
    @JsonProperty("guarantee")
    String guarantee = "";                      // <보증서제공여부>
    @JsonProperty("real_photo")
    String realPhoto = "";                      // <사진촬영 여부>, 실물사진(1)/실물사진 아님(2)

    @JsonProperty("g_photo1_imsi")
    String photo1 = "";                         // <상품사진1> imsi_122948581ed1.jpg
    @JsonProperty("g_photo2_imsi")
    String photo2 = "";                         // <상품사진2>
    @JsonProperty("g_photo3_imsi")
    String photo3 = "";                          // <상품사진3>
    @JsonProperty("g_photo4_imsi")
    String photo4 = "";                          // <상품사진4>
    @JsonProperty("g_photo5_imsi")
    String photo5 = "";                          // <상품사진5>
    @JsonProperty("g_photo6_imsi")
    String photo6 = "";                          // <상품사진6>
    @JsonProperty("g_photo8_imsi")
    String photo8 = "";                          // <상품사진7>
    @JsonProperty("g_photo9_imsi")
    String photo9 = "";                          // <상품사진8>
    @JsonProperty("g_photo10_imsi")
    String photo10 = "";                         // <상품사진9>
    @JsonProperty("g_photo7_imsi")
    String warrantyPhoto = "";                   // <보증서 사진>

    @JsonProperty("g_scrach")
    String goodsScratch = "";                   // <흠(스크래치 정도)>
    @JsonProperty("part1")
    String part1 = "";                          // <보증서(구매영수증)유무>, 보증서 있음(1)/보증서 없음(0)
    @JsonProperty("part2")
    String part2 = "";                          // <부속품-택>,       값=1, 체크박스
    @JsonProperty("part3")
    String part3 = "";                          // <부속품-게런티카드>, 값=1, 체크박스
    @JsonProperty("part4")
    String part4 = "";                          // <부속품-더스트백>,   값=1, 체크박스
    @JsonProperty("part5")
    String part5 = "";                          // <부속품-케이스>,    값=1, 체크박스
    @JsonProperty("part_text")
    String partText = "";                       // <기타 부속품>
    @JsonProperty("a_hd")
    String aHd;                                 // 파악불가, 페이지데이터
    @JsonProperty("b_hd")
    String bHd;                                 // 파악불가, 페이지데이터
    @JsonProperty("e_hd")
    String eHd;                                 // 파악불가, 페이지데이터
    @JsonProperty("card_max")
    String cardMax;                             // 판매자 카드결제 제한, 페이지데이터
    @JsonProperty("card")
    String card;                                // <신용카드 허용여부>, 신용카드 결제허용(1)/신용카드 결제불가능(0)
    @JsonProperty("gift_feelpon")
    String giftFeelwayCoupon;                   // <사은품(필웨이쿠폰)>
    @JsonProperty("commission_payer")
    String commissionPayer;                     // 파악불가, 페이지데이터

    @JsonProperty("sending_nation")
    String sendingNation;                       // <배송-배송위치>, 국내배송(1)/해외배송(2)
    @JsonProperty("sending_payer")
    String sendingPayer;                        // <배송-배송비 부담>, 판매자부담(0)/구매자부담(1)
    @JsonProperty("sending_method")
    String sendingMethod;                       // <배송-배송방법>, 택배(1)/소포.등기(2)/일반우편(3)/기타(4)
    @JsonProperty("sending_price")
    String sendingPrice;                        // <배송-예상 배송비>
    @JsonProperty("sending_period")
    String sendingPeriod = "";                  // <배송-예상 배송기간>
    @JsonProperty("phone")
    String phone = "";                          // <연락처-전화>
    @JsonProperty("hphone")
    String mobilePhone = "";                    // <연락처-휴대폰>
    @JsonProperty("g_intro")
    String goodsIntro = "";                     // <상품 상세설명>
    @JsonProperty("more_up_price")
    String moreUpPrice;                         // 0 뭔지 모르겠다. 사토리얼것도 0으로 세팅되어 있음. 페이지데이터
    @JsonProperty("auto_roll_in")
    String autoRollIn;                          // 0 뭔지 모르겠다. 등록갯수 초과시 1이되는 플래그인것 같다. (초과등록시 요금 2천원 ), 페이지데이터

    public void setGoodsPrice(Long goodsPrice) {
        this.goodsPrice = goodsPrice.toString();
        this.goodsPriceBack = goodsPrice.toString();
        setGoodsPriceKorean(goodsPrice);
    }

    private void setGoodsPriceKorean(Long price) {
        double minimumUnit = 10000.0;
        Long tenThousand = (long) Math.floor(price / minimumUnit);
        Long one = (long) (price % minimumUnit);

        this.goodsPriceKorean = String.format("%s만 %s원", tenThousand, one);
    }

    public void setImage(FeelwayProduct feelwayProductImage) {
        this.photo1 = feelwayProductImage.photo1;
        this.photo2 = feelwayProductImage.photo2;
        this.photo3 = feelwayProductImage.photo3;
        this.photo4 = feelwayProductImage.photo4;
        this.photo5 = feelwayProductImage.photo5;
        this.photo6 = feelwayProductImage.photo6;
        this.photo8 = feelwayProductImage.photo8;
        this.photo9 = feelwayProductImage.photo9;
        this.photo10 = feelwayProductImage.photo10;
        this.warrantyPhoto = feelwayProductImage.warrantyPhoto;

    }

    public void setPart1(boolean warrantyExistenceFlag) {
        this.part1 = warrantyExistenceFlag ? "1" : "0";
    }

    public void setPart2(boolean tagProvisionFlag) {
        this.part1 = tagProvisionFlag ? "1" : "";
    }

    public void setPart3(boolean guaranteeCardProvisionFlag) {
        this.part3 = guaranteeCardProvisionFlag ? "1" : "";
    }

    public void setPart4(boolean dustBagProvisionFlag) {
        this.part4 = dustBagProvisionFlag ? "1" : "";
    }

    public void setPart5(boolean caseProvisionFlag) {
        this.part5 = caseProvisionFlag ? "1" : "";
    }

    public void setCard(boolean cardAvailabilityFlag) {
        this.card = cardAvailabilityFlag ? "1" : "0";
    }

    public void setNewProduct(OnlineSaleDto.ProductCondition productCondition) {
        if (productCondition.equals(OnlineSaleDto.ProductCondition.USED)) {
            this.newProduct = "1";
        } else {
            this.newProduct = "2";
        }
    }

    public void setRealPhoto(Boolean realPhotoFlag) {
        if (Boolean.TRUE.equals(realPhotoFlag)) {
            this.realPhoto = "1";
        } else {
            this.realPhoto = "2";
        }
    }

    @Getter
    public enum Mode {
        CREATE(""),
        UPDATE("modify");

        private String modeName;

        Mode(String modeName) {
            this.modeName = modeName;
        }
    }
}
