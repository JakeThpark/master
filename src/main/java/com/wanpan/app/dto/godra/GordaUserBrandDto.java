package com.wanpan.app.dto.godra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public class GordaUserBrandDto {
    public static class Response {

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GetBrandPage {
            @JsonProperty("totalCount")
            private int totalCount;
            @JsonProperty("pageNo")
            private int pageNo;
            @JsonProperty("pageSize")
            private int pageSize;
            @JsonProperty("result")
            private List<GetBrand> result = new ArrayList<>();
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GetBrand {
            @JsonProperty("id")
            private long brandId;
            @JsonProperty("enName")
            private String brandName;
            @JsonProperty("name")
            private String name;
            @JsonProperty("shoppingGender")
            private String shoppingGender;
            @JsonProperty("topLetter")
            private String topLetter;
            @JsonProperty("imagePath")
            private String imagePath;
            @JsonProperty("productCount")
            private int productCount;
            @JsonProperty("favorites")
            private int favorites;
        }
    }
}
