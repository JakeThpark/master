package com.wanpan.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ShopAccountDto {

    @Data
    @NoArgsConstructor
    public static class Response{
        @JsonProperty("requestId")
        private Long requestId;
        @JsonProperty("loginId")
        private String loginId;
        @JsonProperty("password")
        private String password;
        @JsonProperty("shopType")
        private String shopType;
        @JsonProperty("latestCollectOrderAt")
        private String latestCollectOrderAt;
        @JsonProperty("successFlag")
        private boolean successFlag;
        @JsonProperty("message")
        private String message;

        public Response(String loginId, String password){
            this.loginId =loginId;
            this.password =password;
//            this.shopType =shopType;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        @JsonProperty("requestId")
        private Long requestId;
        @JsonProperty("loginId")
        private String loginId;
        @JsonProperty("password")
        private String password;
        @JsonProperty("shopType")
        private String shopType;
        @JsonProperty("latestCollectOrderAt")
        private String latestCollectOrderAt;
        @JsonProperty("successFlag")
        private boolean successFlag;
        @JsonProperty("message")
        private String message;

        public Request(String loginId, String password){
            this.loginId =loginId;
            this.password =password;
//            this.shopType =shopType;
        }
    }

}
