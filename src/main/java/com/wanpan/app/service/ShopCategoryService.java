package com.wanpan.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopCategoryDto;
import com.wanpan.app.entity.NotificationType;
import com.wanpan.app.entity.Shop;
import com.wanpan.app.entity.ShopCategory;
import com.wanpan.app.repository.NotificationTypeRepository;
import com.wanpan.app.repository.ShopCategoryRepository;
import com.wanpan.app.repository.ShopRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class ShopCategoryService {
    private final ShopServiceFactory shopServiceFactory;
    private final ShopAccountService shopAccountService;
    private final ShopRepository shopRepository;
    private final ShopCategoryRepository shopCategoryRepository;
    private final ObjectMapper objectMapper;
    private final NotificationTypeRepository notificationTypeRepository;

    public int createCategories(String shopType, final long shopAccountId)
            throws GeneralSecurityException, IOException {
        Shop shop = shopRepository.findById(shopType);
        ShopService shopService = shopServiceFactory.getShopService(shopType);
        String token = shopAccountService.getTokenByShopAccountId(shopAccountId);
        List<CategoryDto> categoryDtoList = shopService.getCategoryList(token);

        return createCategories(categoryDtoList, shop.getId(), null);
    }

    private int createCategories(List<CategoryDto> categoryDtoList, String shopId, Long parentId) {
        int count = 0;
        for (CategoryDto categoryDto : categoryDtoList) {
            Long newCategoryId = createCategory(categoryDto, shopId, parentId);
            List<CategoryDto> child = categoryDto.getChild();
            if(!Objects.isNull(child) && !child.isEmpty()) {
                count += createCategories(child, shopId, newCategoryId);
            }

            count++;
        }
        return count;
    }

    private Long createCategory(CategoryDto categoryDto, String shopId, Long parentId) {
        ShopCategory shopCategory = new ShopCategory();
        shopCategory.setName(categoryDto.getName());
        shopCategory.setShopCategoryCode(categoryDto.getId());
        shopCategory.setParentId(parentId);
        shopCategory.setShopId(shopId);

        shopCategoryRepository.save(shopCategory);

        return shopCategory.getId();
    }

    /**
     * Json 파일로부터 쇼핑몰 카테고리 데이터를 업데이트한다.
     */
    public int updateCategoriesFromJsonFile(String shopType) throws IOException {
        String pathName = "";

        switch (shopType) {
            case "FEELWAY":
                pathName = "./category-feelway.json";
                break;
            case "MUSTIT":
                pathName = "./category-mustit.json";
                break;
            case "REEBONZ":
                pathName = "./category-reebonz.json";
                break;
            default:
                break;
        }

        List<ShopCategoryDto> shopCategoryList = objectMapper.readValue(new File(pathName), new TypeReference<>() {});
        Shop shop = shopRepository.findById(shopType);

        return updateCategoriesFromJsonFile(shop.getId(), shopCategoryList, null);
    }

    /**
     * 해당 쇼핑몰 카테고리명을 Key로 하여 업데이트한다.
     */
    private int updateCategoriesFromJsonFile(String shopId, List<ShopCategoryDto> shopCategoryList, Long parentId) {
        int count = 0;

        for (ShopCategoryDto shopCategoryDto : shopCategoryList) {
            String shopCategoryName = shopCategoryDto.getName();
            String notificationType = shopCategoryDto.getNotificationType();
            NotificationType foundNotificationType = notificationTypeRepository.findByName(notificationType);
            ShopCategory foundShopCategory = shopCategoryRepository.findByShopIdAndNameAndParentId(shopId, shopCategoryName, parentId);
            if (foundShopCategory == null) {
                log.error("Not found Shop Category: {}", shopCategoryDto);
                return count;
            }

            foundShopCategory.setNotificationTypeId(foundNotificationType.getId()); // 상품정보고시타입 업데이트

            // 자식이 있다면 자식에게 접근
            if (!shopCategoryDto.getChild().isEmpty()) {
                count += updateCategoriesFromJsonFile(shopId, shopCategoryDto.getChild(), foundShopCategory.getId());
            }

            count++;
        }

        return count;
    }


}
