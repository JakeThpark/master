package com.wanpan.app.dto.godra.seller.order;

import com.wanpan.app.dto.godra.type.CancelreturnReasonType;
import com.wanpan.app.dto.godra.type.CancelreturnStatus;
import com.wanpan.app.dto.godra.type.CancelreturnType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancelreturnDto {

    @Data
    public static class Request {
        @Data
        @AllArgsConstructor
        public static class Create {
            private long orderItemId;
            private CancelreturnType type;
            private CancelreturnReasonType reasonType;
            private String detailReason;
        }

        @Data
        @AllArgsConstructor
        public static class Update {
            private CancelreturnReasonType reasonType;
            private String detailReason;
        }

    }

    @Data
    public static class Response {
        private long id;
        private long userId;
        private CancelreturnType type;
        private CancelreturnStatus status;
        private boolean partial;
        private CancelreturnReasonType reasonType;
        private String detailReason;
        private BigDecimal refundPriceAmount;
        private BigDecimal refundPointAmount;
        private BigDecimal refundAmount;
        protected LocalDateTime refundAt;
        private String refundCardName;
        private String refundBankName;
        private String refundBankDepositor;
        private String refundBankAccount;
        private LocalDateTime createdAt;
        private GordaItemResponse orderItem;
    }
}
