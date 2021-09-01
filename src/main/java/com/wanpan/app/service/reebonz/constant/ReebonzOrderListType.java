package com.wanpan.app.service.reebonz.constant;

/**
 * 리본즈 주문 리스트 범위 타입
 * 주문관련 리스트를 가져올때 상태에 따른 목록을 조회하기 위한 Parameter 값들
 */
public enum ReebonzOrderListType {
    PROCESSING_TOTAL_ORDER("totalOrd"), //전체
    COMPLETE_ORDER("ordCompl"), //주문완료
    DELIVERY_READY("delivReady"), //배송준비중
    DELIVERY_AND_COMPLETE("delivIng"), //배송중(완료)

    CLAIM_TOTAL_ORDER("totalOrd"), //취소, 반품 전체
    CANCEL_REQUEST("ordCancelReq"), //취소요청
    RETURN_REQUEST("ordRefundReq"), //반품요청

    CALCULATION_SCHEDULE("ordCompl"), //정산예정
    CALCULATION_COMPLETE("paymentCompl"), //정산완료
    CANCEL_COMPLETE("ordCanceled"), //취소완료
    RETURN_COMPLETE("ordRefunded") //반품완료
    ;

    private final String code;

    ReebonzOrderListType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
