package com.wanpan.app.service.mustit.constant;

public enum MustitBoldFontPeriod {
    ZERO(0, "0|0"),
    ONE(1, "1|300"),
    SEVEN(7, "7|1400"),
    FIFTEEN(15, "15|2000"),
    THIRTY(30, "30|3000"),
    FIFTY(50, "50|5000");

    private final int period;
    private final String code;

    MustitBoldFontPeriod(int period, String code) {
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
        for (MustitBoldFontPeriod mustitPremiumSalePeriod : MustitBoldFontPeriod.values()) {
            if (mustitPremiumSalePeriod.getPeriod() == period) {
                return mustitPremiumSalePeriod.getCode();
            }
        }

        return null;
    }
}
