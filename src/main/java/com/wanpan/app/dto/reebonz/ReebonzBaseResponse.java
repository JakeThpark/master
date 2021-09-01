package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class ReebonzBaseResponse<T> {
    private String result;
    private String message;
    private T data;
}
