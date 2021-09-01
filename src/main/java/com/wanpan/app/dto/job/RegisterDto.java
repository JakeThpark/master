package com.wanpan.app.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public class RegisterDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends JobTaskResponseBaseDto{

        @JsonProperty("shopSaleId")
        private Long shopSaleId; //요청시 받은 ID
        @JsonProperty("postId")
        private String postId; //쇼핑몰 등록했을때 쇼핑몰 키값
        @JsonProperty("status")
        private ShopSaleJobDto.SaleStatus status; //쇼핑몰 판매글 상태값

        public Response(Long shopSaleId, String postId, ShopSaleJobDto.SaleStatus status, long jobId, long requestId, boolean successFlag, String message){
            this.shopSaleId = shopSaleId;
            this.postId = postId;
            this.status = status;
            this.setJobId(jobId);
            this.setRequestId(requestId);
            this.setSuccessFlag(successFlag);
            this.setMessage(message);
        }
    }



}
