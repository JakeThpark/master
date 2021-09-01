package com.wanpan.app.service.mustit.constant;

public enum MustitDeliveryFeeType {
    NO_CHARGE("0"),
    PAY_ON_DELIVERY("1"),
    PREPAYMENT("2");

    private final String code;

    MustitDeliveryFeeType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
