package com.wanpan.app.dto.mustit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MustitSignInRequest {
    private String eventNo;
    private String redirect;
    private String id;
    private String pw;
    private String saveId;

    public MustitSignInRequest(String id, String pw){
        this.id = id;
        this.pw = pw;
        this.eventNo = "";
        this.redirect = "";
        this.saveId = "1";
    }

}
