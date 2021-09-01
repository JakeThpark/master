package com.wanpan.app.dto.mustit;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MustitCategory {
    //categoryHtml 파싱 결과물을 저장한다.
    private String code;
    private String name;
    private String description;
    private String parentCode;
    private String headerCategory;
    private List<MustitCategory> subCategories;

    public MustitCategory(String code, String name, String parentCode, String headerCategory){
        this.code = code;
        this.name = name;
        this.parentCode = parentCode;
        this.headerCategory = headerCategory;
    }
}
