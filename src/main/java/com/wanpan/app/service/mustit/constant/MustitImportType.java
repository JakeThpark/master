package com.wanpan.app.service.mustit.constant;

public enum MustitImportType {
    PARALLEL_IMPORT("1"),
    FORMAL_IMPORT("2");

    private final String code;

    MustitImportType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
