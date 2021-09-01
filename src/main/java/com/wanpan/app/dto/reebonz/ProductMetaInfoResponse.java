package com.wanpan.app.dto.reebonz;

import lombok.Data;

import java.util.List;

@Data
public class ProductMetaInfoResponse {
    private List<ProductMetaInfo> productMetaInfo;
    private Page page;
}
