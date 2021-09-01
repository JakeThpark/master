package com.wanpan.app.service.mustit.constant;

public enum MustitProductCondition {
    UNUSED("0"),
    USED("1");

    private final String code;

    MustitProductCondition(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
