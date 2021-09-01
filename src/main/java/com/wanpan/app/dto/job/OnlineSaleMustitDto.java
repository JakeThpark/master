package com.wanpan.app.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineSaleMustitDto {
    //판매 추가필드
    private String deliveryType; // "DOMESTIC",
    private String deliveryFeeType; // "NO_CHARGE",
    private int premiumSalePeriod; // 0,
    private int boldFontPeriod; // 0,
    private boolean customsClearanceCodeNecessityFlag; // true,
    private int deliveryFee; // 10000,
    private String detail; // "string"
    private String filterCode; // "string"

}
