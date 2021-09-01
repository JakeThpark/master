package com.wanpan.app.dto.reebonz;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class ReebonzOrderDto {
    public static class Response{
        @Data
        public static class Collect{
            private List<ReebonzOrder> orders;
            private Page page;
        }
    }
    public static class Request{
        @Data
        @NoArgsConstructor
        public static class Collect{
            private String perPage;
            private String currentPage;
            private String orderStatus;
            private String deliveryStatus;
            private String orderedAt;
            private String orderedAtStart;
            private String orderedAtEnd;
            private String filterToReebonz;
        }
    }
}
