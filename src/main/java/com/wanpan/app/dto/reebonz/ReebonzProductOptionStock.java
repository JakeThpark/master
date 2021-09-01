package com.wanpan.app.dto.reebonz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReebonzProductOptionStock {
    /* product[stocks][숫자][id]
     * 형식: 색상값|사이즈값
     */
    private String optionId;

    /* product[stocks][숫자][name]
     * 형식: 색상값|사이즈값
     */
    private String optionName;

    /* product[stocks][숫자][stock_count]
     * 형식: 10
     */
    private String optionQuantity;

    /* product[stocks][숫자][available]
     * 형식: true/false
     */
    private String optionAvailableFlag;
}
