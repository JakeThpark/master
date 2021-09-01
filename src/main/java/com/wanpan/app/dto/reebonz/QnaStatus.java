package com.wanpan.app.dto.reebonz;


public enum QnaStatus {
    ALL("totalqna"),
    OPEN("open"),
    REOPENED("re-opened"),
    CLOSED("closed");

    private final String paramCode;

    QnaStatus(String paramCode) {
        this.paramCode = paramCode;
    }

    public String getParamCode() {
        return this.paramCode;
    }
}
