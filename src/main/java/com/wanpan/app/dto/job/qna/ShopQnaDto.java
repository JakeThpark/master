package com.wanpan.app.dto.job.qna;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.JobTaskResponseBaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * 엔진에서 콜백을 주기위한 DTO
 */
@Data
public class ShopQnaDto {
    public static class Request{
        //수집에 대한 callback 요청
        @Data
        public static class CollectCallback{
            @JsonProperty("shopProductBrand")
            private String shopProductBrand;
            @JsonProperty("shopProductId")
            private String shopProductId;
            @JsonProperty("shopProductSubject")
            private String shopProductSubject;
            @JsonProperty("shopProductLink")
            private String shopProductLink;
            @JsonProperty("shopProductThumbnailLink")
            private String shopProductThumbnailLink;
            @JsonProperty("questionId")
            private String questionId;
            @JsonProperty("questionStatus")
            private ShopQnaJobDto.QuestionStatus questionStatus;
            @JsonProperty("questionTitle")
            private String questionTitle;
            //private LocalDateTime question_time;
            @JsonProperty("postAt")
            private String postAt;
            @JsonProperty("questionWriter")
            private String questionWriter;

            @JsonProperty("possibleSubjectInputFlag")
            private boolean possibleSubjectInputFlag;

            @JsonProperty("shopQnaConversationDtoList")
            private List<ShopQnaConversationDto.Request.CollectCallback> shopQnaConversationDtoList;

            public CollectCallback(){
                this.shopQnaConversationDtoList = new ArrayList<>();
            }
        }

        //post 작업 요청
        @Data
        public static class PostJob{
            private String shopProductBrand;//
            private String shopProductId;
            private String shopProductSubject;//
            private String shopProductLink;//
            private String shopProductThumbnailLink;//
            private String questionId;
            private String questionStatus;//
            private String questionTitle;
            private String postAt;//
            private String questionWriter;
            private ShopQnaConversationDto.Request.PostJob shopQnaConversation;
        }

        //post작업 요청에 대한 callback 요청
        @Data
        public static class PostCallback{
            @JsonProperty("shopProductBrand")
            private String shopProductBrand;
            @JsonProperty("shopProductId")
            private String shopProductId;
            @JsonProperty("shopProductSubject")
            private String shopProductSubject;
            @JsonProperty("shopProductLink")
            private String shopProductLink;
            @JsonProperty("shopProductThumbnailLink")
            private String shopProductThumbnailLink;
            @JsonProperty("questionId")
            private String questionId;
            @JsonProperty("questionStatus")
            private ShopQnaJobDto.QuestionStatus questionStatus;
            @JsonProperty("questionTitle")
            private String questionTitle;
            //private LocalDateTime question_time;
            @JsonProperty("postAt")
            private String postAt;
            @JsonProperty("questionWriter")
            private String questionWriter;

            @JsonProperty("shopQnaConversationDtoList")
            private List<ShopQnaConversationDto.Request.PostCallback> shopQnaConversationDtoList;

            public PostCallback(){
                this.shopQnaConversationDtoList = new ArrayList<>();
            }
        }

    }

    public static class Response{
        public static class CollectCallback{

        }
        public static class PostCallback{

        }
    }

}
