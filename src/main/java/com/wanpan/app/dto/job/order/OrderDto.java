package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrderDto {
    public static class Request{
        //주문수집에 대한 callback 요청
        @Data
        public static class CollectCallback{
            @JsonProperty("orderDate")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime orderDate; // 결제일시
            @JsonProperty("orderId")
            private String orderId; // 주문번호---------필수처리
            @JsonProperty("orderUniqueId")
            private String orderUniqueId; // 주문고유번호---------필수처리
            @JsonProperty("status")
            private OrderStatus status; // 주문상태(상품상태)---------필수처리
            @JsonProperty("postId")
            private String postId; // 상품번호
            @JsonProperty("brandName")
            private String brandName; // 브랜드 명
            @JsonProperty("productName")
            private String productName; // 상품명
            @JsonProperty("memo")
            private String memo; // 사이즈(선택사항) - 색상 : 화이트 / 사이즈 : XL
            @JsonProperty("classificationValue")
            private String classificationValue; // 사이즈(선택사항) 화이트
            @JsonProperty("optionName")
            private String optionName; // 사이즈(선택사항) - XL
            @JsonProperty("quantity")
            private String quantity; // 주문수량

            @JsonProperty("buyerId")
            private String buyerId; // 구매자 아이디
            @JsonProperty("buyerName")
            private String buyerName; // 구매자 이름
            @JsonProperty("buyerPhoneNumber")
            private String buyerPhoneNumber; // 구매자 전화번호
            @JsonProperty("buyerMobilePhoneNumber")
            private String buyerMobilePhoneNumber; // 구매자 모바일 번호
            @JsonProperty("buyerEmail")
            private String buyerEmail; // 구매자 이메일(수령자 이메일로 처리중) - recipientEmail

            @JsonProperty("recipientName")
            private String recipientName; // 수령자 이름
            @JsonProperty("recipientPhoneNumber")
            private String recipientPhoneNumber; // 수령자 전화번호
            @JsonProperty("recipientMobilePhoneNumber")
            private String recipientMobilePhoneNumber; // 수령자 휴대폰 번호
            @JsonProperty("recipientEmail")
            private String recipientEmail; // 수령자 이메일

            @JsonProperty("recipientZipCode")
            private String recipientZipCode; // 우편번호
            @JsonProperty("recipientAddress")
            private String recipientAddress = ""; // 수령자주소
            @JsonProperty("depositor")
            private String depositor; // 입금자 명
            @JsonProperty("personalClearanceCode")
            private String personalClearanceCode; // 개인통관고유부호
            @JsonProperty("price")
            private Long price; // 상품가격
            @JsonProperty("paymentAmount")
            private Long paymentPrice; // 결제금액(공급가)
            @JsonProperty("couponDiscountPrice")
            private Long couponDiscountPrice; //쿠폰가(쿠폰 적용 할인된 금액)
            @JsonProperty("couponPrice")
            private Long couponPrice; //쿠폰금액
            @JsonProperty("courierCode")
            private String courierCode; //택배사 코드
            @JsonProperty("courierName")
            private String courierName; //택배사 이름
            @JsonProperty("courierCustomName")
            private String courierCustomName; //직접입력한 택배사 이름
            @JsonProperty("trackingNumber")
            private String trackingNumber; // 송장번호

            @JsonProperty("gift")
            private String gift; // 사은품
            @JsonProperty("requirements")
            private String requirements; // 기타요구사항
            @JsonProperty("deliveryMessage")
            private String deliveryMessage; // 배송 메세지
            @JsonProperty("deliveryFeeType")
            private String deliveryFeeType; // 배송비 종류(무료배송)
            @JsonProperty("deliveryType")
            private String deliveryType; // 배송종류(국내, 해외)
            @JsonProperty("deliveryFee")
            private String deliveryFee; // 배송비
            @JsonProperty("saleCancellationReason")
            private String saleCancellationReason; // 판매취소 사유 -------처리
            @JsonProperty("purchaseCancellationReason")
            private String purchaseCancellationReason; // 구매취소 사유 -------처리
            @JsonProperty("exchangeReason")
            private String exchangeReason; // 교환 사유 -------처리
            @JsonProperty("returnReason")
            private String returnReason; // 반품 사유 -------처리

            @JsonProperty("officialSku")
            private String officialSku; //공식 SKU(리본즈SKU)
            @JsonProperty("customizedSku")
            private String customizedSku; //판매자 SKU

            @JsonProperty("exchangeCourierCode")
            private String exchangeCourierCode; //교환 택배사 코드
            @JsonProperty("exchangeCourierName")
            private String exchangeCourierName; //교환 택배사 이름
            @JsonProperty("exchangeCourierCustomName")
            private String exchangeCourierCustomName; //직접입력한 교환 택배사 이름
            @JsonProperty("exchangeTrackingNumber")
            private String exchangeTrackingNumber; // 교환 송장번호

            @JsonProperty("returnCourierCode")
            private String returnCourierCode; //반송 택배사 코드
            @JsonProperty("returnCourierName")
            private String returnCourierName; //반송 택배사 이름
            @JsonProperty("returnCourierCustomName")
            private String returnCourierCustomName; //직접입력한 반송 택배사 이름
            @JsonProperty("returnTrackingNumber")
            private String returnTrackingNumber; // 반송 송장번호

            @JsonProperty("calculateDate")
            private String calculateDate; //정산지급 예정일
            @JsonProperty("calculateAmount")
            private String calculateAmount; //정산(예정)금액

            @JsonIgnore
            private long shopDeliveryId; //고르다를 위한 ID저장
            @JsonIgnore
            private long shopCancelreturnId; //고르다를 위한 ID저장

            @JsonProperty("orderBaseConversationList")
            private List<OrderBaseConversationDto> orderBaseConversationList; //주문대화 채널 리스트

            public CollectCallback(){
                this.orderBaseConversationList = new ArrayList<>();
            }
        }
    }

    public static class Response {
        public static class CollectCallback {

        }
    }

    /**
     * 쇼핑몰의 TEXT 상태값들에 대한 callback 상태값
     */
    public enum OrderStatus {
        PAYMENT_NOT_CONFIRM("입금미확인"),
        PAYMENT_COMPLETE("발송요청", "결제완료", "입금확인", "결제확인됨", "주문완료"),
        PAYMENT_COMPLETE_BY_EXCHANGE("발송요청(교환)"),
        DELIVERY_READY("배송준비중", "배송 준비중"),
        DELIVERY_READY_BY_EXCHANGE("배송준비중(교환)"),
        DELIVERY("배송중", "배송 중"),
        DELIVERY_BY_EXCHANGE("배송중(교환)"), //머스트잇 구매취소반려시 배송중(교환) 상태로 변경
        DELIVERY_COMPLETE("배송완료"),
        BUY_CONFIRM("구매확정", "구매결정확인"),
        BUY_CANCEL_REQUEST("구매취소요청","배송전 구매취소 요청","취소요청"),
        BUY_CANCEL_COMPLETE("구매취소완료","취소완료"),
        EXCHANGE_REQUEST("교환요청"),
        RETURN_REQUEST("반품요청"),
        RETURN_CONFIRM("반품성사"),

        //Gorda
        RETURN_RECEIVED_COMPLETED("반품접수완료"), //Gorda RETURN_RECEIVED_COMPLETED
        RETURN_PRODUCT_PICK_UP("반품상품수거"), //Gorda only
        RETURN_PRODUCT_PICK_UP_COMPLETED("반품상품수거완료"), //Gorda only
        RETURN_PRODUCT_CHECK("반품상품검수"), //Gorda only

        RETURN_COMPLETE("반품환불완료","반품완료"),
        RETURN_REJECT("반품거절"),
        RETURN_DELIVERY("반송중"),
        REFUND_COMPLETE("환불완료"),
        REFUND("환불대기중"), //필웨이의 환불대기중은 판매자 반품완료 클릭 후 구매자가 환불요청을 했을때
        SELL_CANCEL("판매취소","거래취소"),
        SELL_CANCEL_COMPLETE("판매취소완료"),
        CALCULATION_SCHEDULE("정산예정","정산요청중"),
        CALCULATION_DELAY("정산보류중"),
        CALCULATION_COMPLETE("정산완료");

        private final List<String> codes;

        OrderStatus(String ...codes) {
            this.codes = Arrays.asList(codes);
        }

        public List<String> getCodes() {
            return this.codes;
        }

        public static OrderStatus getByCode(String code) {
            for (OrderStatus orderStatus : OrderStatus.values()) {
                if (orderStatus.getCodes().contains(code)) {
                    return orderStatus;
                }
            }
            return null;
        }
    }

}
