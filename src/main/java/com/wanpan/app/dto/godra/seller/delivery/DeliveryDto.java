package com.wanpan.app.dto.godra.seller.delivery;

import com.wanpan.app.dto.godra.type.InvoiceStatus;
import com.wanpan.app.dto.godra.type.OrderItemStatus;
import com.wanpan.app.dto.godra.type.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeliveryDto {

    @Data
    public static class Request {

        @Data
        @AllArgsConstructor
        public static class Create {
            private long orderItemId; //송장 입력할 주문 아이템 번호
            private String companyId; //고르다에 매핑된 택배사 아이디
            private String invoiceNumber; //입력할 송장 번호
        }

        @Data
        @AllArgsConstructor
        public static class Update {
            private String companyId; //고르다에 매핑된 택배사 아이디
            private String invoiceNumber; //입력할 송장 번호
        }

    }

    @Data
    public static class Response {

        private long id;
        private String invoiceNumber;
        private InvoiceStatus status;
        private Company company;
        private OrderItem orderItem;

        @Data
        public static class Company {
            private long id;
            private String name;
            private boolean domestic;
            private String code;
        }

        @Data
        public static class OrderItem {
            private long id;
            private OrderItemStatus status;
            private String name;
            private String designerName;
            private String imagePath;
            private String sizeOptionName;
            private int quantity;
            private BigDecimal price;
            private BigDecimal settlePriceAmount;
            private BigDecimal couponDiscountAmount;
            private BigDecimal promotionDiscountAmount;
            private boolean reviewable;
            private OrderItem.Order order;

            @Data
            public static class Order {
                private long id;
                private BigDecimal settlePriceAmount;
                private BigDecimal productPriceAmount;
                private BigDecimal discountPriceAmount;
                private BigDecimal pointDiscountAmount;
                private BigDecimal couponDiscountAmount;
                private BigDecimal promotionDiscountAmount;
                private BigDecimal deliveryFeeAmount;
                private BigDecimal refundPriceAmount;
                private PaymentType paymentType;
                private LocalDateTime payCompleteAt;
                private String pgName;
                private String pgTransactionNumber;
                private String cardName;
                private boolean cardInstallmentEnabled;
                private int cardInstallmentMonths;
                private String bankAccount;
                private String bankDepositor;
                private LocalDateTime bankDepositAt;
                private LocalDateTime createdAt;
                private OrderItem.Order.Info info;

                @Data
                public static class Info {
                    private String name;
                    private String email;
                    private String cellphone;
                    private String roadCode;
                    private String roadAddress;
                    private String extraRoadAddress;
                    private String receiverName;
                    private String receiverCellphone;
                    private String receiverTelephone;
                    private String receiverRoadCode;
                    private String receiverRoadAddress;
                    private String receiverExtraRoadAddress;
                    private String memo;
                }
            }
        }
    }

}
