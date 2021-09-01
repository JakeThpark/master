package com.wanpan.app.dto.reebonz;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DetailImageRequest {
    private String detailImageUrl;

    public DetailImageRequest(String detailImageUrl){
        this.detailImageUrl = detailImageUrl;
    }
}
