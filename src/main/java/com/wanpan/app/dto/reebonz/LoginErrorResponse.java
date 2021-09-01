package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class LoginErrorResponse {
    private String error;
    private String errorDescription;
}
