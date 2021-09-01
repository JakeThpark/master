package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderBaseConversationDto {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("orderUniqueId")
    private String orderUniqueId;
    @JsonProperty("channelId")
    private String channelId;
    @JsonProperty("orderBaseConversationMessageList")
    private List<OrderBaseConversationMessageDto> orderBaseConversationMessageList;

    public OrderBaseConversationDto(){
        this.orderBaseConversationMessageList = new ArrayList<>();
    }
}
