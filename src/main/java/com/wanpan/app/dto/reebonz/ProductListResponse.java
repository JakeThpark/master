package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ProductListResponse {
    private List<Product> products;
    private Page page;
}
