package com.wanpan.app.dto.mustit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MustitProductOptionStock {

    /** opt[]
     * 형식: 색상값|사이즈값
     */
    private String optionName;

    /** stock[]
     * 형식: 10
     */
    private String optionQuantity;

}
