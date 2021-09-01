package com.wanpan.app.service;

import com.wanpan.app.entity.*;
import com.wanpan.app.repository.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TestCategoryService {
    private final TestCategoryRepository testCategoryRepository;
    private final TestShopCategoryRepository testShopCategoryRepository;
    private final TestCategoryMapRepository testCategoryMapRepository;

    public List<ResponseCategory> getCategory(){
        List<TestCategory> categoryList = testCategoryRepository.findByParentId(null);
        return categoryList.stream().map(this::mapToDto).collect(Collectors.toList());
//        return categoryList;
    }

    ResponseCategory mapToDto(TestCategory testCategory)  {
        log.info("testCategory:{}",testCategory);
        ResponseCategory responseCategory = new ResponseCategory();
        responseCategory.setId(testCategory.getId());
        if(testCategory.getChildCategory().size() > 0){
            for(TestCategory childCategory : testCategory.getChildCategory()){
                responseCategory.getChild().add(mapToDto(childCategory));
            }
        }
        responseCategory.setName(testCategory.getName());
        responseCategory.setNotificationType(testCategory.getNotificationType().getName());
        for(TestCategoryMap testCategoryMap : testCategory.getCategoryMaps()){
            ResponseCategoryMap responseCategoryMap = new ResponseCategoryMap();
            responseCategoryMap.setShopType(testCategoryMap.getShop().getId());
            responseCategoryMap.setShopCategoryId(testCategoryMap.getShopCategoryId());
            responseCategory.getShopCategories().add(responseCategoryMap);
        }
        return responseCategory;
    }


    public List<ResponseShopCategory> getShopCategory(String shopId){
        List<TestShopCategory> categoryList = testShopCategoryRepository.findByShopIdAndParentId(shopId, null);
        return categoryList.stream().map(this::mapToDto).collect(Collectors.toList());
//        return categoryList;
    }

    ResponseShopCategory mapToDto(TestShopCategory testShopCategory)  {
        log.info("testShopCategory:{}",testShopCategory);
        ResponseShopCategory responseShopCategory = new ResponseShopCategory();
        responseShopCategory.setId(testShopCategory.getId());
        responseShopCategory.setShopCategoryCode(testShopCategory.getShopCategoryCode());
        if(testShopCategory.getChildShopCategory().size() > 0){
            for(TestShopCategory childShopCategory : testShopCategory.getChildShopCategory()){
                responseShopCategory.getChild().add(mapToDto(childShopCategory));
            }
        }
        responseShopCategory.setName(testShopCategory.getName());
        responseShopCategory.setNotificationType(testShopCategory.getNotificationType().getName());
        return responseShopCategory;
    }

    @Data
    @NoArgsConstructor
    public static class  ResponseCategory{
        private long id;
        private String name;
        private String notificationType;
        List<ResponseCategoryMap> shopCategories = new ArrayList<>();
        List<ResponseCategory> child = new ArrayList<>();

    }

    @Data
    @NoArgsConstructor
    public static class  ResponseCategoryMap{
        private String shopType;
        private long shopCategoryId;
    }

    @Data
    @NoArgsConstructor
    public static class  ResponseShopCategory{
        private long id;
        private String name;
        private String notificationType;
        private String shopCategoryCode;
        List<ResponseShopCategory> child = new ArrayList<>();
    }






}
