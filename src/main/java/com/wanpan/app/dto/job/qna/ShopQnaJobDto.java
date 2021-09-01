package com.wanpan.app.dto.job.qna;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.JobTaskResponseBaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShopQnaJobDto {
    public static class Request{
        //수집 작업 요청
        @Data
        public static class CollectJob{
            @JsonProperty("questionStatus")
            private QuestionStatus questionStatus;
            @JsonProperty("shopAccounts")
            private List<ShopAccountDto.Request> shopAccounts;
        }
        //답변등록 작업 요청
        @Data
        public static class PostJob {
            @JsonProperty("shopAccount")
            private ShopAccountDto.Request shopAccount;
            @JsonProperty("shopQna")
            private ShopQnaDto.Request.PostJob shopQna;
        }


        //수집 작업 결과 콜백요청
        @Data
        public static class CollectCallback {
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;

            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;

            @JsonProperty("shopQnAList")
            private List<ShopQnaDto.Request.CollectCallback> shopQnAList;

            public CollectCallback(){
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
                this.shopQnAList = new ArrayList<>();

            }
        }

        //답변등록 작업 결과 콜백요청
        @Data
        @AllArgsConstructor
        public static class PostCallback {
            @JsonProperty("jobTaskResponseBaseDto")
            private JobTaskResponseBaseDto jobTaskResponseBaseDto;

            @JsonProperty("shopAccount")
            private ShopAccountDto.Response shopAccount;

            @JsonProperty("shopQnAList")
            private List<ShopQnaDto.Request.PostCallback> shopQnAList;

            public PostCallback(){
                this.jobTaskResponseBaseDto = new JobTaskResponseBaseDto();
                this.shopQnAList = new ArrayList<>();

            }
        }
    }

    @Getter
    public enum QuestionStatus{
        READY, COMPLETE;
    }
}
