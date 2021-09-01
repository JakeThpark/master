package com.wanpan.app.dto.feelway;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FeelwayOrderList {
    private List<FeelwayOrder> data = new ArrayList<>();
}
