package com.wanpan.app.service;

import com.wanpan.app.dto.job.*;
import com.wanpan.app.entity.ShopCategory;
import com.wanpan.app.repository.ShopCategoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ShopProductService {
    private final ShopCategoryRepository shopCategoryRepository;
    private final ModelMapper modelMapper;

    public OnlineSaleDto getShopProduct() {
//        ShopCategory shopCategory = shopCategoryRepository.findById(249L).get();
        ShopCategory shopCategory = shopCategoryRepository.findById(101L).get();
        ShopCategoryDto shopCategoryDto = modelMapper.map(shopCategory, ShopCategoryDto.class);
        log.info("shopCategory: {}", shopCategory);
        log.info("shopCategoryDto: {}", shopCategoryDto);

        OnlineSaleFeelwayDto onlineSaleFeelwayDto = new OnlineSaleFeelwayDto();
//        onlineSaleFeelwayDto.getCategoryList().add(shopCategoryDto);

        OnlineSaleMustitDto onlineSaleMustitDto = new OnlineSaleMustitDto();

        OnlineSaleReebonzDto onlineSaleReebonzDto = new OnlineSaleReebonzDto();

        OnlineSaleDto onlinesaleDto = new OnlineSaleDto();
//        onlinesaleDto.setOnlineSaleFeelway(onlineSaleFeelwayDto);
//        onlinesaleDto.setOnlineSaleMustit(onlineSaleMustitDto);
//        onlinesaleDto.setOnlineSaleReebonz(onlineSaleReebonzDto);
//
//        ShopAccountDto.Request request = new ShopAccountDto.Request();
//        onlinesaleDto.getShopAccountList().add(request);
//        onlinesaleDto.getShopAccountList().add(request);
//
//        onlinesaleDto.getOnlineSaleImageList().add(new OnlineSaleImageDto());
//        onlinesaleDto.getOnlineSaleImageList().add(new OnlineSaleImageDto());
//
//        onlinesaleDto.getProductList().add(new ProductDto());
//        onlinesaleDto.getProductList().add(new ProductDto());


        return onlinesaleDto;
    }
}
