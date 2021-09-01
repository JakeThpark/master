package com.wanpan.app.dto.mustit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MustitProductImage {

    /** delete_img[]
     * 이미지 삭제 여부
     * 형식: 삭제안함(0), 삭제함(1)
     */
    private String deleteFlag = null;

    /** imgSrc[]
     * 이미지 소스 URL
     */
    private String src = null;

}
