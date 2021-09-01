package com.wanpan.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.entity.Category;
import com.wanpan.app.entity.NotificationType;
import com.wanpan.app.repository.CategoryRepository;
import com.wanpan.app.repository.NotificationTypeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class SellistCategoryService {
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final NotificationTypeRepository notificationTypeRepository;

    public Integer createCategories() throws IOException {
        JsonNode fileJsonNode = objectMapper.readValue(new File("./sellist-category-sample.json"), JsonNode.class);
        for (int i = 0; i < fileJsonNode.size(); i++) {
            JsonNode eachNode = fileJsonNode.get(i);
            log.info("{}", eachNode);

            Long parentId = null;

            String genderNodeValue = eachNode.get("성별").asText();
            String depth1NodeValue = eachNode.get("대").asText();
            String depth2NodeValue = eachNode.get("중").asText();
            String depth3NodeValue = eachNode.get("소").asText();

            //빈값이 있을 경우 처리하지 않는다.
            if (genderNodeValue.length() == 0 || depth1NodeValue.length() == 0
                    || depth2NodeValue.length() == 0 || depth3NodeValue.length() == 0) {
                log.info("Data is not valid. Not processing!: {}", eachNode);
                continue;
            }

            //성별 처리
            Category genderCategory = checkAndCreateCategory(genderNodeValue, parentId);

            //대분류 처리
            parentId = genderCategory.getId();
            Category depth1Category = checkAndCreateCategory(depth1NodeValue, parentId);

            //중분류 처리
            parentId = depth1Category.getId();
            Category depth2Category = checkAndCreateCategory(depth2NodeValue, parentId);

            //소분류 처리
            parentId = depth2Category.getId();
            Category depth3Category = checkAndCreateCategory(depth3NodeValue, parentId);
        }

        return fileJsonNode.size();
    }

    /**
     * 카테고리 데이터에 대해서 존재 여부를 검토한 후에 없는 경우 생성한다.
     */
    private Category checkAndCreateCategory(String name, Long parentId) {
        Category findCategory = categoryRepository.findByNameAndParentId(name, parentId);
        //동일 데이터가 없는 경우 만들어준다.
        if (ObjectUtils.isEmpty(findCategory)) {
            findCategory = new Category();
            findCategory.setName(name);
            findCategory.setParentId(parentId);
            findCategory = categoryRepository.save(findCategory);
        }

        return findCategory;
    }

    /**
     * Json 파일로부터 셀리스트 카테고리 데이터를 생성하거나 업데이트한다.
     */
    public int createOrUpdateCategoriesFromJsonFile() throws IOException {
        List<CategoryDto> categoryList =
                objectMapper.readValue(new File("./sellist-category.json"), new TypeReference<>() {});

        return createOrUpdateCategoriesFromJsonFile(null, categoryList);
    }

    /**
     * 해당 카테고리 ID가 없으면 생성하고, 있으면 상품고시정보를 업데이트한다.
     */
    private int createOrUpdateCategoriesFromJsonFile(Long parentId, List<CategoryDto> categoryList) {
        int count = 0;

        for (CategoryDto categoryDto : categoryList) {
            long parentIdOfChildren;
            NotificationType foundNotificationType = notificationTypeRepository.findByName(categoryDto.getNotificationType());

            if (categoryDto.getId() == null) {
                // 해당 카테고리 ID가 없으면 생성
                Category category = new Category();
                category.setName(categoryDto.getName());
                category.setNotificationTypeId(foundNotificationType.getId());
                category.setParentId(parentId);

                categoryRepository.save(category); // 생성

                parentIdOfChildren = category.getId();
            } else {
                // 해당 카테고리 ID가 있으면 상품고시정보를 업데이트
                Category foundCategory = categoryRepository.findById(Long.valueOf(categoryDto.getId())).orElse(null);
                if (foundCategory == null) {
                    log.error("Not found Category ID: {}", categoryDto.getId());
                    return count;
                }

                foundCategory.setNotificationTypeId(foundNotificationType.getId()); // 업데이트

                parentIdOfChildren = foundCategory.getId();
            }

            if (!categoryDto.getChild().isEmpty()) {
                // 자식이 있다면 자식에게 접근
                count += createOrUpdateCategoriesFromJsonFile(parentIdOfChildren, categoryDto.getChild());
            }

            count++;
        }

        return count;
    }

}
