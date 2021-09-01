package com.wanpan.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ShopNoticeDto {
    private String shopNoticeId;
    private String subject;
    private String writer;
    private LocalDateTime registeredDate;
    private String contents;
}
