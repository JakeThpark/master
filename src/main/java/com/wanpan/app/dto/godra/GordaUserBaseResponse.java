package com.wanpan.app.dto.godra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GordaUserBaseResponse<T> {
    @JsonProperty("code")
    private String code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("responseAt")
    private String responseAt;
    @JsonProperty("response")
    private T response;
}
