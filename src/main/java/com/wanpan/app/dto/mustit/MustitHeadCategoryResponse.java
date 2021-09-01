package com.wanpan.app.dto.mustit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class MustitHeadCategoryResponse {
    private String headerCategory;//"W,M"
    @JsonProperty("isDisplay")
    private String isDisplay;//"Y"
    private String number;//"875"
    private String thread;//"29" //categoryCode
    private String title;//"가방"
    private String titleEn;//"Bag"
}
