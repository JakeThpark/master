package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class Stock {

    private long id;
    private String name;
    private int stock_count;
    private int ordered_count;
    private int current_count;
}
