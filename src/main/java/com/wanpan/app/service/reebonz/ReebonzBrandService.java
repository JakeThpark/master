package com.wanpan.app.service.reebonz;

import com.wanpan.app.config.gateway.ReebonzClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.reebonz.BrandListResponse;
import com.wanpan.app.dto.reebonz.ReebonzBaseResponse;
import com.wanpan.app.entity.Brand;
import com.wanpan.app.repository.BrandRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ReebonzBrandService {
    private final ReebonzClient reebonzClient;

    public List<BrandDto> getBrandListBySearchName(String shopToken, String searchName) {
        try {
            log.info("shopToken: {}", shopToken);

            ReebonzBaseResponse<BrandListResponse> brandListResponse
                    = reebonzClient.getBrands(shopToken, searchName);

            if ("success".equals(brandListResponse.getResult())) {
                log.debug("get Data Success!!");
            } else {
                log.debug("get Data Fail!!");
            }
            return brandListResponse.getData().getBrands().stream()
                    .map(brand -> new BrandDto(brand.getBrandId(), brand.getBrandName()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Fail~", e);
            return null;
        }

    }

}
