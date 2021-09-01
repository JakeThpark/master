package com.wanpan.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.controller.BrandController;
import com.wanpan.app.dto.BrandDto;

import com.wanpan.app.entity.*;
import com.wanpan.app.repository.BrandMapRepository;
import com.wanpan.app.repository.BrandNotMappingRepository;
import com.wanpan.app.repository.BrandRepository;
import com.wanpan.app.repository.ShopRepository;
import com.wanpan.app.service.reebonz.ReebonzBrandService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class BrandService {
    private ShopServiceFactory shopServiceFactory;
    private ShopRepository shopRepository;
    private BrandRepository brandRepository;
    private BrandMapRepository brandMapRepository;
    private BrandNotMappingRepository brandNotMappingRepository;
    private ShopAccountService shopAccountService;
    private ReebonzBrandService reebonzBrandService;

    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;

    /*
     * 최초 우리의 기본이 되는 브랜드를 리본즈로 부터 가져와서 입력한다.
     * 동일한 이름이 없을 경우는 product_brand_map_temp 테이블에 입력해서 추후 수동 처리가 되게 한다.
     */
    public List<BrandDto> initBrandFromReebonz(BrandController.BrandRequest brandRequest) throws GeneralSecurityException, IOException {
        log.info("CALL initBrandFromReebonz");
        ShopService shopService = shopServiceFactory.getShopService(brandRequest.getShopType());
        ShopAccountToken.Type tokenType = ShopAccountToken.Type.SESSION;
        if("REEBONZ".equals(brandRequest.getShopType())){
            tokenType = ShopAccountToken.Type.BEARER;
        }
        String token = shopService.getToken(brandRequest.getUsername(), brandRequest.getPassword(), tokenType);

        List<BrandDto> brandDtoList = reebonzBrandService.getBrandListBySearchName(token, "");
        log.info("reebonz brands data: {}", brandDtoList);

        List<Brand> targetBrandList = new ArrayList<>();
        for (BrandDto reebonzBrand : brandDtoList) {
            //DB에 같은 이름을 가진 브랜드가 있는지 확인해서 없는 경우 추가한다.
            int duplicateCount = brandRepository.countByName(reebonzBrand.getBrandName());
            if (duplicateCount == 0) {
                targetBrandList.add(new Brand(reebonzBrand.getBrandName()));
            }
        }
        log.info("targetBrandList:{}",targetBrandList);
        //중복되지 않은 브랜드 리스트 저장
        brandRepository.saveAll(targetBrandList);
        return brandDtoList;
    }

    /*
     * 쇼핑몰 타입에 맞춰 쇼핑몰들의 브랜드를 가져와서 매핑작업을 수행한다
     * 동일한 이름이 없을 경우는 product_brand_map_temp 테이블에 입력해서 추후 수동 처리가 되게 한다.
     */
    public List<BrandDto> mappingShopBrandByShopType(BrandController.BrandRequest brandRequest) {
        log.info("CALL mappingShopBrandByShopType");
        try {
            ShopService shopService = shopServiceFactory.getShopService(brandRequest.getShopType());
            ShopAccountToken.Type tokenType = ShopAccountToken.Type.SESSION;

            if("REEBONZ".equals(brandRequest.getShopType())){
                tokenType = ShopAccountToken.Type.BEARER;
            }
            String token = shopService.getToken(brandRequest.getUsername(), brandRequest.getPassword(), tokenType);

            //해당 사이트에서 Brand를 Get 한다.
            List<BrandDto> brandList = shopService.getBrandList(token);
            log.info("brands data: {}", camelObjectMapper.writeValueAsString(brandList));
            
            Shop shop = shopRepository.findById(brandRequest.getShopType());

            for(BrandDto brand : brandList){
                //DB에 같은 이름을 가진 브랜드가 있는지 확인한다.(존재 할 경우 연결시키고, 없을 경우는 수동처리를 할수 있도록 한다
                Brand duplicateBrand = brandRepository.findByName(brand.getBrandName());
                //찾지 못했을 경우 Not Mapping에 저장한다.
                if(ObjectUtils.isEmpty(duplicateBrand)){
                    //중복이 아닐때 저장한다.
                    List<BrandNotMapping> brandNotMappingList = brandNotMappingRepository.findByShopIdAndSourceCodeAndSourceName(shop.getId(), String.valueOf(brand.getBrandId()), brand.getBrandName());
                    if(ObjectUtils.isEmpty(brandNotMappingList)){
                        brandNotMappingRepository.save(new BrandNotMapping(shop.getId(), String.valueOf(brand.getBrandId()), brand.getBrandName()));
                    }
                    continue;
                }

                //찾았을 경우 Map 중복 체크 후 저장
                BrandMap findBrandMap = brandMapRepository.findByBrandIdAndShopId(duplicateBrand.getId(), shop.getId());
                if(ObjectUtils.isEmpty(findBrandMap)){
                    brandMapRepository.save(
                            new BrandMap(duplicateBrand.getId(), shop.getId(),String.valueOf(brand.getBrandId()), brand.getBrandName())
                    );
                }
            }
            return null;
        } catch (Exception e){
            log.error("Failed mapping shop brand",e);
            return null;
        }
    }

    /*
     * 각각의 쇼핑몰들의 브랜드를 가져와서 매핑작업을 수행한다
     * 동일한 이름이 없을 경우는 product_brand_map_temp 테이블에 입력해서 추후 수동 처리가 되게 한다.
     */
    public List<BrandDto> mappingEachShopBrandByShopType(String shopType, final long shopAccountId) {
        log.info("CALL mappingEachShopBrandByShopType");
        try {
            ShopService shopService = shopServiceFactory.getShopService(shopType);
            String token = shopAccountService.getTokenByShopAccountId(shopAccountId);
            //해당 사이트에서 Brand를 Get 한다.
            List<BrandDto> brandList = shopService.getBrandList(token);
            log.info("brands data: {}", brandList);

            Shop shop = shopRepository.findById(shopType);
            for(BrandDto brand : brandList){
                //DB에 같은 이름을 가진 브랜드가 있는지 확인한다.(존재 할 경우 연결시키고, 없을 경우는 수동처리를 할수 있도록 한다
                Brand duplicateBrand = brandRepository.findByName(brand.getBrandName());
                //찾지 못했을 경우 Not Mapping에 저장한다.
                if(ObjectUtils.isEmpty(duplicateBrand)){
                    //중복이 아닐때 저장한다.
                    List<BrandNotMapping> brandNotMappingList = brandNotMappingRepository.findByShopIdAndSourceCodeAndSourceName(shop.getId(), String.valueOf(brand.getBrandId()), brand.getBrandName());
                    if(ObjectUtils.isEmpty(brandNotMappingList)){
                        brandNotMappingRepository.save(new BrandNotMapping(shop.getId(), String.valueOf(brand.getBrandId()), brand.getBrandName()));
                    }
                    continue;
                }

                //찾았을 경우 Map 중복 체크 후 저장
                BrandMap findBrandMap = brandMapRepository.findByBrandIdAndShopId(duplicateBrand.getId(), shop.getId());
                if(ObjectUtils.isEmpty(findBrandMap)){
                    brandMapRepository.save(
                            new BrandMap(duplicateBrand.getId(), shop.getId(),String.valueOf(brand.getBrandId()), brand.getBrandName())
                    );
                }
            }
            return null;
        } catch (Exception e){
            return null;
        }
    }


}
