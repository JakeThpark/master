package com.wanpan.app.dto.godra.seller.my;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerInfoResponse {
    private long id;
    private String companyName;
    private String companyRepresentative;
    private String companyRegistrationNumber;
    private String onlineMarketingNumber;
    private String contact;

//    private CalculateType calculateType;
    private String calculateType;
    private BigDecimal commissionRate;
//    private BankType bankType;
    private String bankType;
    private String bankDepositor;
    private String bankAccount;
}
