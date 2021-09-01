package com.wanpan.app.dto.job.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderBaseConversationMessageDto {
    @JsonProperty("type")
    private Type type;
    @JsonProperty("content")
    private String content;
    @JsonProperty("postAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postAt;
    @JsonProperty("shopMessageId")
    private String shopMessageId;

    public enum Type {
        BUYER, SELLER, SHOP;
    }
}
