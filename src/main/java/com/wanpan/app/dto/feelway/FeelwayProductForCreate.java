package com.wanpan.app.dto.feelway;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class FeelwayProductForCreate extends FeelwayProduct {
    private static final String ZERO = "0";
    @JsonProperty("text")
    String text = "2배보상하겠음";                 // 입력받는 값은 아님.
    @JsonProperty("copy_g_no")
    String copyGoodsId = "";                    // copy 기능 사용시 복사한 상품의 id
    @JsonProperty("copy_g_name")
    String copyGoodsName = "";                  // copy 기능 사용시 복사한 상품의 이름

    @JsonProperty("g_method")
    String goodsMethod;                         // <파워 정품 등록>, 추가하지 않음(0)/20일(2)/7일(2)
    @JsonProperty("plus_price")
    String plusPrice;                           // <파워 정품 등록-추가요금>
    @JsonProperty("power_period")
    String powerPeriod = "";                    // <파워 정품 등록-기간>, g_method 와 연관되서 업데이트 됨
    @JsonProperty("settle_price")
    String settlePrice;                         // <등록 수수료 합계>

    @JsonProperty("u_money")
    String uMoney;                              // <나의 eMoney 잔액>, 페이지데이터
    @JsonProperty("need_price")
    String needPrice;                           // 현재 사용하지 않는 값으로 추정, 페이지데이터

    public void setPowerPeriod(Integer powerPeriod) {
        int moreUpPrice = Integer.parseInt(this.moreUpPrice);
        int autoRollIn = Integer.parseInt(this.autoRollIn);

        long settlePrice = moreUpPrice + (autoRollIn * 2000L);
        if (Objects.isNull(powerPeriod) || powerPeriod == 0) {
            this.goodsMethod = ZERO;
            this.plusPrice = ZERO;
        } else if (powerPeriod > 7) {
            this.powerPeriod = Integer.toString(powerPeriod);
            this.goodsMethod = "2";
            this.plusPrice = "10000";
        } else {
            this.powerPeriod = Integer.toString(powerPeriod);
            this.goodsMethod = "2";
            this.plusPrice = "5000";
        }

        long resultSettlePrice = settlePrice + Long.parseLong(this.plusPrice);
        this.settlePrice = Long.toString(resultSettlePrice);
    }
}
