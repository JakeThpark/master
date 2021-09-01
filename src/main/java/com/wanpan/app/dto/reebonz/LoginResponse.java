package com.wanpan.app.dto.reebonz;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private long createdAt;
}
