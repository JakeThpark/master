package com.wanpan.app.service.mustit.constant;

public enum MustitDeliveryType {
    DOMESTIC("국내배송", "0"),
    OVERSEAS("해외배송", "1"),
    OVERSEAS_DIRECT("해외직배송", "2");

    private final String name;
    private final String code;

    MustitDeliveryType(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public static MustitDeliveryType getByName(String name) {
        for (MustitDeliveryType mustitDeliveryType : MustitDeliveryType.values()) {
            if (mustitDeliveryType.getName().equals(name)) {
                return mustitDeliveryType;
            }
        }

        return DOMESTIC;
    }
}
