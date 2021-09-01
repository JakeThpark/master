package com.wanpan.app.service.mustit.constant;

public enum MustitSaleStatus {
    ON_SALE("판매중"),
    SALE_STOP("판매중지"),
    SOLD_OUT("일시품절"),
    NOT_FOUND_SALE("삭제된글");

    private final String code;

    MustitSaleStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static MustitSaleStatus getByCode(String code) {
        for (MustitSaleStatus mustitSaleStatus : MustitSaleStatus.values()) {
            if (mustitSaleStatus.getCode().equals(code)) {
                return mustitSaleStatus;
            }
        }
        return null;
    }
}
