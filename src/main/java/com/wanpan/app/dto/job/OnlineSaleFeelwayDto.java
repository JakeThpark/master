package com.wanpan.app.dto.job;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class OnlineSaleFeelwayDto {
    //판매 추가필드
    private FeelwayDeliveryMethod deliveryMethod; // "DELIVERY",
    private FeelwayDeliveryPayer deliveryPayer; // "SELLER",
    private FeelwayDeliveryType deliveryType; // "DOMESTIC",
    private boolean cardAvailabilityFlag; // true,
    private int giftFeelponCount; // 0,
    private String damage; // "string",
    private boolean warrantyFlag; // true,
    private boolean tagProvisionFlag; // true,
    private boolean guaranteeCardProvisionFlag; // true,
    private boolean dustBagProvisionFlag; // true,
    private boolean caseProvisionFlag; // true,
    private String otherItem; // "string",
    private int powerSalePeriod; // 0,
    private String buyYear; // "string",
    private String buyPlace; // "string",
    private String expectedDeliveryFee; // "string",
    private String expectedDeliveryPeriod; // "string",
    private String detail; // "string"

    @Getter
    public enum FeelwayDeliveryType {
        DOMESTIC("1"),
        OVERSEAS("2");

        private String originValue;
        FeelwayDeliveryType(String originValue) {
            this.originValue = originValue;
        }
    }

    @Getter
    public enum FeelwayDeliveryPayer {
        SELLER("0"),
        BUYER("1");

        private String originValue;
        FeelwayDeliveryPayer(String originValue) {
            this.originValue = originValue;
        }
    }

    @Getter
    public enum FeelwayDeliveryMethod {
        DELIVERY("1"),
        REGISTERED_MAIL("2"),
        REGULAR_MAIL("3"),
        ETC("4");

        private String originValue;

        FeelwayDeliveryMethod(String originValue) {
            this.originValue = originValue;
        }
    }
}
