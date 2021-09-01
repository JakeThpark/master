package com.wanpan.app.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineSaleImageDto {
    private String originImagePath;
    private int sequence; // 이미지 순서
    private boolean mainFlag; // false
}
