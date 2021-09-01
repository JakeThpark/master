package com.wanpan.app.dto.godra.seller.order;

import com.wanpan.app.dto.godra.type.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GordaItemResponse {

    private long id;
//    private OrderItemStatus status;
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
    private Order order;
    private Promotion promotion;
    private ShopProduct shopProduct;
    private UserCoupon userCoupon;
    private Invoice invoice;
    private Cancelreturn cancelreturn;
    private Calculation calculation;

    @Data
    public static class UserCoupon {
        private long id;
        private Coupon coupon;
        private LocalDateTime issueStartAt;
        private LocalDateTime issueEndAt;
        private LocalDateTime currentAt = LocalDateTime.now();
        private boolean used;

        @Data
        public static class Coupon {
            private long id;
//            private CouponType type;
            private String type;
            private String name;
            private String description;
//            private CouponBenefitType benefitType;
            private String benefitType;
            private BigDecimal benefit;
            private boolean maxBenefitEnabled;
            private BigDecimal maxBenefit;
        }
    }

    @Data
    public static class Promotion {
        private long id;
        private BigDecimal discountPercent;
        private BigDecimal discountPrice;
        private BigDecimal price;
        private boolean expired;
    }

    @Data
    public static class ShopProduct {
        private long id;
        private BigDecimal quantity;
        private BigDecimal price;
        private ShopProductStatus status;
        private Product product;

        @Data
        public static class Product {
            private long id;
            private String name;
        }
    }

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
//        private PaymentType paymentType;
        private String paymentType;
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
        private Info info;

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

    @Data
    public static class Invoice {
        private long id;
        private String invoiceNumber;
//        private InvoiceStatus status;
        private String status;
        private Company company;

        @Data
        public static class Company {
            private long id;
            private String name;
            private boolean domestic;
            private String code;
        }
    }

    @Data
    public static class Cancelreturn {
        private long id;
//        private CancelreturnType type;
        private String type;
//        private CancelreturnStatus status;
        private String status;
        private boolean partial;
        private CancelreturnReasonType reasonType;
//        private String reasonType;
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
    }

    @Data
    public static class Calculation {
        private long id;
        private CalculateType type;
        private CalculateStatus status;
        private BigDecimal commissionRate;
        private BigDecimal commission;
        private BigDecimal priceAmount;
        private BigDecimal paymentAmount;
        private BigDecimal deliveryFeeAmount;
        private LocalDateTime expectedPaymentAt;
        private LocalDateTime paymentAt;
    }

}
