package com.wanpan.app.dto.job.qna;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShopQnaConversationDto {
    public static class Request{
        @Data
        public static class CollectCallback{
            @JsonProperty("type")
            private String type; //질문,답변
            @JsonProperty("questionType")
            private String questionType;//질문유형
            @JsonProperty("questionId")
            private String questionId;//문의번호(537726)
            @JsonProperty("writerId")
            private String writerId;//작성자 ID(1066420)
            @JsonProperty("writerName")
            private String writerName;//작성자 이름(amhoking)
            @JsonProperty("subject")
            private String subject;//제목
            @JsonProperty("contentImagePath")
            private String contentImagePath;//내용에 이미지 첨부된 경우 경로
            @JsonProperty("content")
            private String content;//내용
            @JsonProperty("conversationId")
            private long conversationId;//대화 고유 키값 혹은 순서
            @JsonProperty("postAt")
            private String postAt;//작성시간(2020. 06. 30 13:14:57)
        }

        @Data
        public static class PostJob{
            private long requestId; //DB에 선 저장된 요청 ID
            private String type; //질문,답변
            private String questionType;//질문유형
            private String questionId;//문의번호(537726)
            private String writerId;//작성자 ID(1066420)
            private String writerName;//작성자 이름(amhoking)
            private String subject;//제목
            private String contentImagePath;//내용에 이미지 첨부된 경우 경로
            private String content;//내용
            private long conversationId;//대화 고유 키값 혹은 순서
            private String postAt;//작성시간(2020. 06. 30 13:14:57)
        }

        @Data
        public static class PostCallback{
            @JsonProperty("type")
            private String type; //질문,답변
            @JsonProperty("questionType")
            private String questionType;//질문유형
            @JsonProperty("questionId")
            private String questionId;//문의번호(537726)
            @JsonProperty("writerId")
            private String writerId;//작성자 ID(1066420)
            @JsonProperty("writerName")
            private String writerName;//작성자 이름(amhoking)
            @JsonProperty("subject")
            private String subject;//제목
            @JsonProperty("contentImagePath")
            private String contentImagePath;//내용에 이미지 첨부된 경우 경로
            @JsonProperty("content")
            private String content;//내용
            @JsonProperty("conversationId")
            private long conversationId;//대화 고유 키값 혹은 순서
            @JsonProperty("postAt")
            private String postAt;//작성시간(2020. 06. 30 13:14:57)
        }
    }
}
