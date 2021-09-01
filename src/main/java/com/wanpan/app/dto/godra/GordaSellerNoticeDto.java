package com.wanpan.app.dto.godra;

import lombok.Data;
import lombok.NoArgsConstructor;

public class GordaSellerNoticeDto {
    public static class Request{

    }
    public static class Response{
        @Data
        @NoArgsConstructor
        public static class NoticeSummary{
            //id*	integer($int64) 공지사항 id
            private Integer id;
            //deleted* boolean 공지사항 삭제 여부
            private boolean deleted;
            //title* string 공지사항 제목
            private String title;
            //createdAt* string($date) 공지사항 생성 일
            private String createdAt;
        }
    }
}
