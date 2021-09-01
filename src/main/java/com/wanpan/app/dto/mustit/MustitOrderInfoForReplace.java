package com.wanpan.app.dto.mustit;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MustitOrderInfoForReplace {
    Map<String, String> orderIdMap;
    Map<String, String> orderStatusMap;
    Map<String, MustitDeliveryInfo> deliveryInfoMap;
    Map<String, MustitDeliveryInfo> exchangeDeliveryInfoMap;
    Map<String, MustitDeliveryInfo> returnDeliveryInfoMap;

    public MustitOrderInfoForReplace() {
        this.orderIdMap = new HashMap<>();
        this.orderStatusMap = new HashMap<>();
        this.deliveryInfoMap = new HashMap<>();
        this.exchangeDeliveryInfoMap = new HashMap<>();
        this.returnDeliveryInfoMap = new HashMap<>();
    }
}
