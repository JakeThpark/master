package com.wanpan.app.dto.godra.seller.order;

import com.wanpan.app.dto.godra.type.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class GordaItemRequest {
    @Data
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private OrderItemStatus status;
    }
}
