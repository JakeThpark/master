package com.wanpan.app.dto.reebonz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReebonzWebPageProductCreateResponse {
    /*
     * product post의 경우 리턴되는 형태가 달라서 별도로 사용한다.
     */
    @JsonProperty("result")
    private String result;
    @JsonProperty("notice")
    private String notice;
    @JsonProperty("dbid")
    private Long dbId;

}
