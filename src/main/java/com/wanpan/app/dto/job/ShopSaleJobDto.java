package com.wanpan.app.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.qna.ShopQnaConversationDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ShopSaleJobDto {

    public static class Request{
        @Data
        @NoArgsConstructor
        public static class PostJob{
            private long id;
            private ShopAccountDto.Request shopAccount;
            private ShopCategoryDto shopCategory;
            private String postId; //고르다의 샵별 product_id
            private String shopProductId; //고르다의 product_id
            private String ticketCode;
            private SaleStatus status;
        }

        @Data
        @NoArgsConstructor
        public static class UpdateSaleStatusJob{
            private long id;
            private ShopAccountDto.Request shopAccount;
            private String postId;
            private SaleStatus requestSaleStatus;
        }

        @Data
        @NoArgsConstructor
        public static class DeleteSaleJob{
            @JsonProperty("id")
            private long id;
            @JsonProperty("shopAccount")
            private ShopAccountDto.Request shopAccount;
            @JsonProperty("postId")
            private String postId;
        }

        //Delete 작업 요청에 대한 callback 요청
        @Data
        public static class DeleteCallback{
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;

            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;

            @JsonProperty("postId")
            private String postId;

            public DeleteCallback(){
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
            }
        }

    }

    /**
     * 쇼핑몰의 판매글 상태값
     */
    public enum SaleStatus {
        ON_SALE, SOLD_OUT, SALE_STOP, SALE_HOLD, READY,
        DELETE, // 삭제 요청에 대해서 판매글 상태(실제 모두 삭제처리 case)
        NOT_FOUND_SALE //삭제를 제외한 요청에서 발생하는 쇼핑몰 판매글이 없는 경우를 나타내는 상태(삭제된글)
    }
}
