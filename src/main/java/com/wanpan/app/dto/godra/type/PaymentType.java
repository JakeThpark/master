package com.wanpan.app.dto.godra.type;

import lombok.Getter;

@Getter
public enum PaymentType {
    DEPOSIT("무통장입금"), CREDIT_CARD("신용카드"), BANK_TRANSFER("계좌이체"), PHONE("휴대폰결제"), VIRTUAL_ACCOUNT("가상계좌"), POINT("포인트");

    private final String description;

    PaymentType(final String description) {
        this.description = description;
    }
}
