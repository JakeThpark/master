package com.wanpan.app.dto.godra;

import com.wanpan.app.service.gorda.constant.GordaGenderType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public class GordaUserCategoryDto {
    public static class Response{
        @Data
        @NoArgsConstructor
        @EqualsAndHashCode(callSuper = true)
        public static class GetCategory extends BaseCategory{
            private GordaGenderType shoppingGender;
            private int productCount;

            private BaseCategory parent;
            private BaseCategory standard;
        }

        @Data
        @NoArgsConstructor
        public static class BaseCategory{
            private long id;
            private String name;
            private String enName;
        }
    }
}
