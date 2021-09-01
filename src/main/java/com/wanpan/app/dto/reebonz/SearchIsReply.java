package com.wanpan.app.dto.reebonz;


public enum SearchIsReply {
    TOTAL("totalcomment"),
    NEW("ready"),
    ADD_NEW("reopen"),
    COMPLETE("complete");

    private final String paramCode;

    SearchIsReply(String paramCode) {
        this.paramCode = paramCode;
    }

    public String getParamCode() {
        return paramCode;
    }
}
