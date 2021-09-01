package com.wanpan.app.dto.feelway;

import com.wanpan.app.dto.job.ShopSaleJobDto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeelwaySellingProduct {
    private String productNumber;
    private String brand;
    private String subject;
    private ShopSaleJobDto.SaleStatus saleStatus;

    public enum FeelwaySaleStatus {
        ON_SALE("판매중"),
        SOLD_OUT("판매완료"),
        SALE_STOP("일시품절"),
        SALE_HOLD("판매보류");


        private final String code;

        FeelwaySaleStatus(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }

        public static FeelwaySaleStatus getByCode(String code) {
            for (FeelwaySaleStatus feelwaySaleStatus : FeelwaySaleStatus.values()) {
                if (feelwaySaleStatus.getCode().equals(code)) {
                    return feelwaySaleStatus;
                }
            }
            return null;
        }

        public static ShopSaleJobDto.SaleStatus convertToShopSaleStatus(FeelwaySaleStatus feelwaySaleStatus){
            return ShopSaleJobDto.SaleStatus.valueOf(feelwaySaleStatus.name());
        }
    }
}
