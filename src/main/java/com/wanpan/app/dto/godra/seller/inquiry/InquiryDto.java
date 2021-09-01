package com.wanpan.app.dto.godra.seller.inquiry;

import com.wanpan.app.dto.godra.type.ShoppingGender;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InquiryDto {
    @Data
    public static class Request {
        @Data
        @AllArgsConstructor
        public static class CreateAnswer {
            private String content; //답변내용
            private boolean deleted; //삭제/숨김 여부
        }

        @Data
        @AllArgsConstructor
        public static class UpdateAnswer {
            private String content; //답변내용
            private boolean deleted; //삭제/숨김 여부
        }
    }
    @Data
    public static class Response {
        private long id;
        private String question;
        private int totalAnswer;
        private User user;
        private Product product;
        private Answer answer;
        private LocalDateTime createdAt;

        @Data
        public static class User {
            private String nickname;
            private String email;
            private String profileImagePath;
        }

        @Data
        public static class Product {
            private long id;
            private String name;
            private ShoppingGender shoppingGender;
            private String designerName;
            private String sizeName;
            // detail
            private String modelNumber;
            private String designerColor;
            private String composition;
            private String description;
            private List<Product.Image> images = new ArrayList<>();

            @Data
            public static class Image {
                private long id;
                private int sequence;
                private String path;
                private boolean main;
            }
        }

        @Data
        public static class Answer {
            private long id;
            private String content;
            private boolean deleted;
            private LocalDateTime createdAt;
        }
    }
}
