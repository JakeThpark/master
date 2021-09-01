package com.wanpan.app.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobTaskResponseBaseDto {
    @JsonProperty("jobId")
    private Long jobId; //요청 Job ID
    @JsonProperty("requestId")
    private Long requestId;
    @JsonProperty("successFlag")
    private boolean successFlag; //성공여부
    @JsonProperty("message")
    private String message; //추가 메세지(실패시에 메세지를 전송해서 전달목적)
}
