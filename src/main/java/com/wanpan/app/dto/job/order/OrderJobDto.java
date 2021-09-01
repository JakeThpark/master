package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.JobTaskResponseBaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class OrderJobDto {
    public static class Request {
        //수집 작업 요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class CollectJob {
            @JsonProperty("orderStatus")
            private OrderProcessStatus orderProcessStatus;
            @JsonProperty("shopAccounts")
            private List<ShopAccountDto.Request> shopAccounts;
        }

        //수집 작업 결과 콜백요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class CollectCallback {
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;

            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;

            @JsonProperty("orderList")
            private List<OrderDto.Request.CollectCallback> orderList;

            public CollectCallback(){
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
                this.orderList = new ArrayList<>();
            }
        }

        //주문대화 전송 작업 요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class PostConversationJob {
            @JsonProperty("shopAccount")
            private ShopAccountDto.Request shopAccount;
            //주문에 대한 정보
            @JsonProperty("shopOrderId")
            private String shopOrderId;
            @JsonProperty("shopUniqueOrderId")
            private String shopUniqueOrderId;
            @JsonProperty("channelId")
            private String channelId;
            @JsonProperty("orderConversationMessage")
            private String orderConversationMessage;
        }

        //주문대화 전송 결과 콜백요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class PostConversationCallback extends OrderBaseConversationJobDto.Request.CollectCallback {}


        //상태 변경 작업 요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class UpdateJob {
            @JsonProperty("shopAccount")
            private ShopAccountDto.Request shopAccount;
            //주문에 대한 정보
            @JsonProperty("shopOrderId")
            private String shopOrderId;
            @JsonProperty("shopUniqueOrderId")
            private String shopUniqueOrderId;
            @JsonProperty("sellerMessage")
            private String sellerMessage;
            //택배사
            @JsonProperty("courier")
            private CourierDto courier;
            //송장번호
            @JsonProperty("trackingNumber")
            private String trackingNumber;
            //변경 요청 주문상태(상품상태)
            @JsonProperty("status")
            private OrderUpdateActionStatus status;
        }

        //상태변경 작업 결과 콜백요청
        @Data
        @EqualsAndHashCode(callSuper=false)
        public static class UpdateCallback {
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;
            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;
            @JsonProperty("orderList")
            private List<OrderDto.Request.CollectCallback> orderList;

            public UpdateCallback() {
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
                this.orderList = new ArrayList<>();
            }
        }

        public enum OrderUpdateActionStatus {
            DELIVERY_READY,//배송준비중 버튼(필웨이없음)
            DELIVERY,//배송중 버튼 및 송장업데이트(배송정보변경)
            EXCHANGE_CONFIRM, //교환승인 버튼(머스트잇만 현재 존재)
            EXCHANGE_REJECT, //교환거절 버튼(머스트잇만 현재 존재)
            BUY_CANCEL_CONFIRM, //구매취소 승인 버튼(필웨이에선 판매취소와 같은 기능)
            BUY_CANCEL_REJECT, //구매취소 거절 버튼
            SELL_CANCEL, //판매취소 버튼
            RETURN_CONFIRM, //반품승인 버튼(쇼핑몰에 반품완료처리)
            RETURN_REJECT, //반품거절 버튼
            CALCULATION_SCHEDULE, //정산요청
            CALCULATION_DELAY //정산보류(필웨이 없음)
        }
    }

    @Getter
    public enum OrderProcessStatus {
        NEW, COMPLETE;
    }
}
