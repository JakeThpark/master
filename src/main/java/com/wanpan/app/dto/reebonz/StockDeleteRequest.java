package com.wanpan.app.dto.reebonz;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockDeleteRequest {
    private long stockId;

    public StockDeleteRequest(long stockId){
        this.stockId = stockId;
    }
}
