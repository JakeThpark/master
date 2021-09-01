package com.wanpan.app.dto.godra;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GordaSellerTokenDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel("GordaSellerTokenDto.Request")
    public static class Request{
        @JsonProperty("username")
        private String username;
        @JsonProperty("password")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @ApiModel("GordaSellerTokenDto.Response")
    public static class Response{
        //접근 토큰
        @JsonProperty("accessToken")
        private String accessToken;
    }
}
