package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.JobTaskResponseBaseDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderBaseConversationJobDto {
    public static class Request {
        //수집 작업 요청
        @Data
        public static class CollectJob {
            @JsonProperty("orderConversationStatus")
            private OrderConversationStatus orderConversationStatus;
            @JsonProperty("shopAccounts")
            private List<ShopAccountDto.Request> shopAccounts;
        }

        //수집 작업 결과 콜백요청
        @Data
        public static class CollectCallback {
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;

            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;

            @JsonProperty("orderBaseConversationList")
            private List<OrderBaseConversationDto> orderBaseConversationList; //주문대화 채널 리스트

            public CollectCallback(){
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
                this.orderBaseConversationList = new ArrayList<>();
            }
        }
    }

    public enum OrderConversationStatus {
        ALL, NEW, COMPLETE;
    }
}
