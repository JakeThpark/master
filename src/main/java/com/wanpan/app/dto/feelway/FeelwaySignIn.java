package com.wanpan.app.dto.feelway;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeelwaySignIn {
    private String loginId;
    private String loginPassword;
}
