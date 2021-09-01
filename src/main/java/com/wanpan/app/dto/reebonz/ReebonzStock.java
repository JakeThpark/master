package com.wanpan.app.dto.reebonz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReebonzStock {
    private String optionGroupName; //옵션 그룹명(실제 등록시 크게 의미 없음)
    private String optionName; //옵션 명
    private int stockCount; //옵션 수
}
