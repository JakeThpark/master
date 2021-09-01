package com.wanpan.app.controller;

import com.wanpan.app.service.SellistCategoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping({"/sellist"})
@AllArgsConstructor
public class SellistController {
    private final SellistCategoryService sellistCategoryService;

    @PostMapping(value = "/categories")
    public ResponseEntity<Integer> createCategories(
    ) throws IOException {
        return ResponseEntity.ok(sellistCategoryService.createCategories());
    }

    @PutMapping(value = "/categories")
    public ResponseEntity<Integer> updateCategories(
    ) throws IOException {
        return ResponseEntity.ok(sellistCategoryService.createOrUpdateCategoriesFromJsonFile());
    }

}
