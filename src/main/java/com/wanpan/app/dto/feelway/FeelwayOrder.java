package com.wanpan.app.dto.feelway;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class FeelwayOrder {
    @JsonProperty("주문번호")
    private String orderId; // 주문번호
    @JsonProperty("결제일시")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime orderDate; // 결제일시
    @JsonProperty("주문상태")
    private String status; // 주문상태
    @JsonProperty("상품번호")
    private String postId; // 상품번호
    @JsonProperty("브랜드 명")
    private String brandName; // 브랜드 명
    @JsonProperty("상품명")
    private String productName; // 상품명
    @JsonProperty("주문수량")
    private String quantity; // 주문수량
    @JsonProperty("사이즈")
    private String memo; // 사이즈(메모내용)
    @JsonProperty("구매자 아이디")
    private String buyerId; // 구매자 아이디
    @JsonProperty("수령자 이름")
    private String recipientName; // 수령자 이름
    @JsonProperty("수령자 전화번호")
    private String recipientPhoneNumber; // 수령자 전화번호
    @JsonProperty("수령자 휴대폰 번호")
    private String recipientMobilePhoneNumber; // 수령자 휴대폰 번호
    @JsonProperty("수령자 이메일")
    private String recipientEmail; // 수령자 이메일
    @JsonProperty("우편번호")
    private String recipientZipCode; // 우편번호

    @JsonProperty("수령자주소")
    private String recipientAddress; // 수령자주소
    @JsonProperty("입금자 명")
    private String depositor; // 입금자 명
    @JsonProperty("개인통관고유부호")
    private String personalClearanceCode; // 개인통관고유부호
    @JsonProperty("상품가격")
    private Long price; // 상품가격
    @JsonProperty("송장번호")
    private String trackingNumber; // 송장번호
    @JsonProperty("송장번호 반송")
    private String returnTrackingNumber; // 송장번호 반송
    @JsonProperty("사은품")
    private String gift; // 사은품
    @JsonProperty("기타요구사항")
    private String requirements; // 기타요구사항
    @JsonProperty("배송종류")
    private String deliveryFeeType; // 배송종류
    @JsonProperty("배송비")
    private String deliveryFee; // 배송비
}
