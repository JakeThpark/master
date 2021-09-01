package com.wanpan.app.dto.godra.type;

import lombok.Getter;

@Getter
public enum OrderItemStatus {
    BEFORE_PAYMENT("결제 진행전", false, false, false, false, false, false),
    DEPOSIT_WAITING("입금대기", false, false, true, false, false, false),
    DEPOSIT_AUTO_CANCELLATION("입금대기 자동취소", false, false, false, false, false, false),
    PAYMENT_FINISHED("결제완료", false, false, true, true, false, false),
    PAYMENT_CANCELING("결제취소요청", false, false, false, false, true, false),
    PAYMENT_CANCELLATION("결제취소", true, false, false, false, false, false),
    PRODUCT_PREPARATION("상품준비", false, false, false, true, false, false),
    SHIPPING("배송", false, false, false, false, false, true),
    DELIVERY_COMPLETED("배송완료", false, false, false, false, false, true),
    PURCHASE_COMPLETED("구매완료", true, false, false, false, false, false),

    RETURN_RECEIVED("반품접수", false, false, false, false, true, false),
    RETURN_RECEIVED_COMPLETED("반품접수완료", false, false, false, false, true, false),
    RETURN_PRODUCT_PICK_UP("반품상품수거", false, false, false, false, true, false),
    RETURN_PRODUCT_PICK_UP_COMPLETED("반품상품수거완료", false, false, false, false, true, false),
    RETURN_PRODUCT_CHECK("반품상품검수", false, false, false, false, true, false),
    RETURN_COMPLETED("반품완료", true, true, false, false, false, false),

    EXCHANGING("교환", false, false, false, false, true, false),
    EXCHANGE_PICK_COMPLETE("교환수거완료", false, false, false, false, true, false),
    EXCHANGE_RESEND("교환재발송", false, false, false, false, true, false),
    EXCHANGE_WITHDRAW("교환거절", false, false, false, false, true, false),
    EXCHANGE_COMPLETE("교환완료", true, true, false, false, false, false);

    private final String name;
    private final boolean completed; // 완료상태
    private final boolean shopCompleted; // 샵이 완료처리한 상태
    private final boolean cancelable;  // 구매자가 취소 가능한 상태
    private final boolean shopCancelable; // 샵이 구매취소 가능한 상태
    private final boolean canCompleteCancelReturn; // 구매자가 취소 접수한 상태- 접수후 완료 처리 가능 상태
    private final boolean canCompletePurChase; // 구매자가 구매완료 가능 상태

    OrderItemStatus(final String name,
                    final boolean completed,
                    final boolean shopCompleted,
                    final boolean cancelable,
                    final boolean shopCancelable,
                    final boolean canCompleteCancelReturn,
                    final boolean canCompletePurChase
    ) {
        this.name = name;
        this.completed = completed;
        this.shopCompleted = shopCompleted;
        this.cancelable = cancelable;
        this.shopCancelable = shopCancelable;
        this.canCompleteCancelReturn = canCompleteCancelReturn;
        this.canCompletePurChase = canCompletePurChase;
    }


}
