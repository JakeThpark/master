package com.wanpan.app.controller;

import com.wanpan.app.service.CategoryService;
import com.wanpan.app.service.TestCategoryService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping({"/test"})
@AllArgsConstructor
public class TestController {
    private TestCategoryService testCategoryService;
    private CategoryService categoryService;

    @GetMapping(value = "/category")
    @ApiOperation(value="셀리스트 카테고리", notes = "셀리스트 카테고리")
    public ResponseEntity<List<TestCategoryService.ResponseCategory>> getCategory(
    ) throws IOException {

        return ResponseEntity.ok(testCategoryService.getCategory());
    }

    @GetMapping(value = "/shop-category")
    @ApiOperation(value="셀리스트 카테고리", notes = "셀리스트 카테고리")
    public ResponseEntity<List<TestCategoryService.ResponseShopCategory>> getShopCategory(
            @RequestParam(value = "shop-id") String shopId
    ) throws IOException {

        return ResponseEntity.ok(testCategoryService.getShopCategory(shopId));
    }

    /**
     * 셀리스트 카테고리에서 고르다 카테고리로 복사해서 매핑까지 추가하기 위한 인터페이스
     * @return
     */
    @GetMapping(value = "/gorda-mapping-category")
    @ApiOperation(value="고르다 카테고리 입력", notes = "고르다 샵 카테고리와 매핑을 추가한다.")
    public ResponseEntity<Void> excuteToInsertGordaShopCategory(){

        return ResponseEntity.ok(categoryService.convertSellistCategoryToGordaCategory());
    }
}
