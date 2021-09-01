package com.wanpan.app.controller;

import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.service.BrandService;
import com.wanpan.app.service.ShopAccountService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping({"/brand"})
@AllArgsConstructor
public class BrandController {
    private final ShopAccountService shopAccountService;
    private final BrandService brandService;

    /*
     * 최초 브랜드 기준 데이타를 입력한다.
     * 리본즈 브랜드 리스트를 기준 데이타로 저장한다.
     */
    @PostMapping(value = "/init")
    @ApiOperation(value="최초 브랜드 목록 저장", notes = "리본즈 브랜드를 기준데이타로 입력한다.")
    public ResponseEntity<List<BrandDto>> createBrand(
            @RequestBody BrandRequest brandRequest)
            throws GeneralSecurityException, IOException {
        return ResponseEntity.ok(brandService.initBrandFromReebonz(brandRequest));
    }

    @PostMapping(value = "/shop-brand-mapping")
    @ApiOperation(value="쇼핑몰별 브랜드 초기 목록 매핑", notes = "쇼핑몰별 브랜드를 우리 브랜드들에 기준 데이타 매핑한다")
    public ResponseEntity<List<BrandDto>> mappingBrand(
            @RequestBody BrandRequest brandRequest) {
        return ResponseEntity.ok(brandService.mappingShopBrandByShopType(brandRequest));
    }

    @Data
    @NoArgsConstructor
    public static class BrandRequest {
        private String shopType;
        private String username;
        private String password;
    }
}
