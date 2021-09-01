package com.wanpan.app.controller;

import com.wanpan.app.service.ShopCategoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@Slf4j
@RequestMapping({"/shop"})
@AllArgsConstructor
public class ShopController {
    private ShopCategoryService shopCategoryService;

    @PostMapping(value = "/categories/{shopType}")
    public ResponseEntity<Integer> createCategories(
            @PathVariable String shopType,
            @RequestParam Long shopAccountId
    ) throws IOException, GeneralSecurityException {
        return ResponseEntity.ok(shopCategoryService.createCategories(shopType, shopAccountId));
    }


    @PatchMapping(value = "/categories/{shopType}")
    public ResponseEntity<Integer> updateCategories(
            @PathVariable String shopType
    ) throws IOException {
        return ResponseEntity.ok(shopCategoryService.updateCategoriesFromJsonFile(shopType));
    }

}
