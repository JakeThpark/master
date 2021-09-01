package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReebonzOrder {
    @JsonProperty("number")
    private String orderId; //주문번호 - 문서는 int이나 실제값은 String

    @JsonProperty("ordered_item_id")
    private long orderUniqueId; //주문아이템번호: 76881,

    @JsonProperty("ordered_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime orderDate; //주문일시:  "2016-03-22 17:29:18",주문일시

    @JsonProperty("product_name")
    private String productName; //주문 상품명: "개발팀테스트",

    @JsonProperty("product_code")
    private String officialSku; //주문 상품 sku코드: "devTeST_0001",

    @JsonProperty("marketplace_product_code")
    private String customizedSku; //주문 상품 마켓 상품코드(판매자SKU), null허용:  null,

    @JsonProperty("order_status")
//    private String orderStatus; //주문 상태: "취소완료",
    private String status; //주문 상태: "취소완료",

    @JsonProperty("product_selling_price")
    private int price; //판매가(실제 구매자가 결제하는 금액): 1000,

    @JsonProperty("product_option_name")
    private String memo; //옵션명(옵션그룹:옵션이름 조합으로 나옴):  "사이즈:26",

    @JsonProperty("quantity")
    private int quantity; //주문수량: 1,

    @JsonProperty("order_user")
    private String buyerName; //구매인: "최광선",

    @JsonProperty("address")
    private String address; // "배송지 주소(우편번호 포함): [04794] 서울시 성동구 아차산로 113번지 삼진빌딩 7층",

    @JsonProperty("recipient")
    private String recipientName; //수령인: "리본즈",

    @JsonProperty("phone")
    private String recipientPhone; //수령인 연락처: "02-3444-5610",

    @JsonProperty("extra_request")
    private String otherRequirements; //주문 메세지: "",

    @JsonProperty("product_id")
    private long postId; //상품 ID:  900666,

    @JsonProperty("delivery_extra_request")
    private String deliveryMessage; //배송 메세지: ""

    //TODO:미사용 필드에 대한 사용여부 확인필요
    //미사용
    @JsonProperty("marketplace_product_name")
    private String marketplaceProductName; //주문 상품 마켓 상품명(판매자 상품명 - 상품명 복사본이고 수정가능), null허용:  null,
    //미사용
    @JsonProperty("product_supply_price")
    private int productSupplyPrice; //공급가 or 판매가(파트너 타입에 따라 변경 - 판매자가 정산되어지는 금액): 900,
    //미사용
    @JsonProperty("reebonz_account")
    private String reebonzAccount; //리본즈 결제 상태값(무통장 입급완료,무통장 초과입금,무통장 미입금): null,
    //미사용
    @JsonProperty("delivery_status")
    private String deliveryStatus; //배송 상태: "배송준비중",
    //미사용
    @JsonProperty("is_substitution")
    private Boolean isSubstitution; //의미모름: false,
    //미사용
    @JsonProperty("clearance_number")
    private String clearanceNumber; //의미모름: null,
}
