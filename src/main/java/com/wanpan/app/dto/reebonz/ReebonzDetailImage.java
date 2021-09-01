package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class ReebonzDetailImage {
    private String detailImageUrl; //상품 이미지 URL

    public ReebonzDetailImage(String detailImageUrl){
        this.detailImageUrl = detailImageUrl;
    }
}
