package com.wanpan.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public class TwoCaptchaDto {
    public static class Request{
        @Data
        @NoArgsConstructor
        public static class Captcha {
            private String key;
            private String method = "post";
            private MultipartFile file;
        }

        @Data
        @NoArgsConstructor
        public static class Result {
            private String key;
            private String action = "get";
            private String id;
        }
    }
}
