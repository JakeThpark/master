package com.wanpan.app.service.reebonz.constant;

public enum ReebonzSaleStatus {
    ON_SALE,
    SALE_STOP,
    SOLD_OUT,
    NOT_FOUND_SALE;

    public static ReebonzSaleStatus getBySaleStatusAndQuantity(String saleStatus, Integer quantity) {
        ReebonzSaleStatus reebonzSaleStatus = null;
        switch (saleStatus) {
            case "판매중":
                reebonzSaleStatus = quantity >= 1 ? ON_SALE : SOLD_OUT;
                break;
            case "판매중지":
                reebonzSaleStatus = SALE_STOP;
                break;
            case "삭제된글":
                reebonzSaleStatus = NOT_FOUND_SALE;
                break;
            default:
                break;
        }
        return reebonzSaleStatus;
    }
}
