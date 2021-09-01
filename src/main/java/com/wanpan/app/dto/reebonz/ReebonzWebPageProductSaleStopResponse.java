package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReebonzWebPageProductSaleStopResponse {
    /*
     * product 판매중지 post의 경우 리턴되는 형태가 달라서 별도로 사용한다.
     */
    @JsonProperty("result")
    private String result;
    @JsonProperty("message")
    private String message;
    @JsonProperty("product_name")
    private String productName;

}
