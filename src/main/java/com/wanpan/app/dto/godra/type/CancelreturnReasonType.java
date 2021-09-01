package com.wanpan.app.dto.godra.type;

import lombok.Getter;

@Getter
public enum CancelreturnReasonType {
    RETURN_CHANGEMIND("단순 변심"),
    RETURN_DIFFERENTOPT("색상/사이즈가 기대와 다름"),
    RETURN_CHEAPER("타 사이트의 가격이 더 저렴함"),
    RETURN_WRONGOPT("상품 옵션 선택을 잘못함"),
    RETURN_DEFECT("상품 결함이 있음"),
    RETURN_INACCURATE("실제 상품이 상품 설명과 다름"),
    RETURN_BOTHDAMAGED("포장과 상품 모두 훼손됨"),
    RETURN_SHIPBOXOK("포장은 괜찮으나 상품이 파손됨"),
    RETURN_PARTIALMISS("주문상품 중 일부 상품이 배송되지 않음"),
    RETURN_COMPOMISS("구성품, 부속품이 제대로 들어있지 않음"),
    RETURN_WRONGDELIVERY("주문과 아예 다른 상품이 배송됨"),
    RETURN_WRONGSIZECOL("주문과 색상/사이즈가 다른 상품이 배송됨"),
    RETURN_LATEDELIVERED("상품이 늦게 배송됨"),
    CANCEL_CHANGEMIND("단순 변심"),
    CANCEL_CHEAPER("타 사이트의 가격이 더 저렴함"),
    CANCEL_WRONGOPT("상품 옵션 선택을 잘못함"),
    CANCEL_SOLD_OUT("품절"),
    CANCEL_ETC("기타"),
    EXCAHNGE_DIFFERENTOPT("색상/ 사이즈가 기대와 다름"),
    EXCAHNGE_PARTIALMISS("주문상품 중 일부 상품이 배송되지 않음"),
    EXCAHNGE_COMPOMISS("구성품, 부속품이 제대로 들어있지 않음"),
    EXCAHNGE_DEFECT("상품 결함이 있음"),
    EXCAHNGE_BOTHDAMAGED("포장과 상품 모두 훼손됨"),
    EXCAHNGE_SHIPBOXOK("포장은 괜찮으나 상품이 파손됨"),
    EXCAHNGE_WRONGDELIVERY("주문과 아예 다른 상품이 배송됨"),
    EXCAHNGE_WRONGSIZECOL("주문과 색상/사이즈가 다른 상품이 배송됨")
    ;

    private final String description;

    CancelreturnReasonType(final String description) {
        this.description = description;
    }


}
