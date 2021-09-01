package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CourierDto {
    @JsonProperty("code")
    private String code; //쇼핑몰 택배사 select option 값
    @JsonProperty("name")
    private String name; //택배사 이름
    @JsonProperty("customName")
    private String customName; //직접 입력시 택배사 이름
}
