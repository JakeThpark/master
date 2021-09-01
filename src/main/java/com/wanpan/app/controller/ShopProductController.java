package com.wanpan.app.controller;

import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.service.ShopProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping({"/shop-product"})
@AllArgsConstructor
public class ShopProductController {
    private final ShopProductService shopProductService;

    @GetMapping
    public ResponseEntity<OnlineSaleDto> getShopProduct() {
        return ResponseEntity.ok(shopProductService.getShopProduct());
    }
}
