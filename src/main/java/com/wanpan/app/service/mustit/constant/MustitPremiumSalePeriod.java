package com.wanpan.app.service.mustit.constant;

public enum MustitPremiumSalePeriod {
    ZERO(0, "0|0"),
    ONE(1, "1|700"),
    SEVEN(7, "7|3500"),
    FIFTEEN(15, "15|6000"),
    THIRTY(30, "30|10000"),
    FIFTY(50, "50|15000");

    private int period;
    private String code;

    MustitPremiumSalePeriod(int period, String code) {
        this.period = period;
        this.code = code;
    }

    public int getPeriod() {
        return period;
    }

    public String getCode() {
        return code;
    }

    public static String getCodeByPeriod(int period) {
        for (MustitPremiumSalePeriod mustitPremiumSalePeriod : MustitPremiumSalePeriod.values()) {
            if (mustitPremiumSalePeriod.getPeriod() == period) {
                return mustitPremiumSalePeriod.getCode();
            }
        }

        return null;
    }
}
