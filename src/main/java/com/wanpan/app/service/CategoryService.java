package com.wanpan.app.service;

import com.wanpan.app.entity.Category;
import com.wanpan.app.entity.CategoryMap;
import com.wanpan.app.entity.ShopCategory;
import com.wanpan.app.entity.ShopCategoryForInsert;
import com.wanpan.app.repository.*;
import com.wanpan.app.service.reebonz.ReebonzBrandService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class CategoryService {
    private CategoryRepository categoryRepository;
    private ShopCategoryForInsertRepository shopCategoryForInsertRepository;
    private CategoryMapRepository categoryMapRepository;

    public Void convertSellistCategoryToGordaCategory(){
        final long ID_MAX_GAP = 3000;
        final String SHOP_TYPE = "GORDA";
        List<Category> categoryList = categoryRepository.findAll();
        //sellist 전체 카테고리를 가져온다.
        for(Category category : categoryList){
            //ShopCategoy 입력======================
            ShopCategoryForInsert shopCategory = new ShopCategoryForInsert();
            shopCategory.setId(category.getId() + ID_MAX_GAP); //ID에 고정값을 더해서 넣어준다.
            shopCategory.setShopId(SHOP_TYPE);
            shopCategory.setName(category.getName());
            shopCategory.setDescription(category.getName());
            shopCategory.setShopCategoryCode(String.valueOf(category.getId() + ID_MAX_GAP));//샾 카테고리 코드는 중복이 안되는게 좋기 때문에 ID로 더미로 넣어준다.
            shopCategory.setNotificationTypeId(category.getNotificationTypeId());
            if(category.getParentId() == null){
                shopCategory.setParentId(null); //부모가 없는 최상위 노드의 경우 null을 넣어준다.
            }else{
                shopCategory.setParentId(category.getParentId() + ID_MAX_GAP); //ID에 고정값을 더해서 넣어준다.
            }
            shopCategory.setCreateAt(LocalDateTime.now());
            shopCategory.setUpdateAt(LocalDateTime.now());
            shopCategoryForInsertRepository.save(shopCategory);

            //CategoyMap 입력======================
            CategoryMap categoryMap = new CategoryMap();
            categoryMap.setCategoryId(category.getId());
            categoryMap.setShopId(SHOP_TYPE);
            categoryMap.setShopCategoryId(shopCategory.getId());
            categoryMap.setShopCategoryCode(shopCategory.getShopCategoryCode());
            categoryMapRepository.save(categoryMap);
        }
        return null;
    }

}
