package com.wanpan.app.service.mustit.parser;

import com.wanpan.app.service.mustit.constant.MustitRequiredCookies;

import java.util.HashMap;
import java.util.Map;

public class MustitTokenParser {
    private static final String DELIMITER = ";";

    public static Map<String, String> parse(String token) {
        Map<String, String> cookies = new HashMap<>();
        String[] requiredCookies = token.split(DELIMITER);

        for (String cookie : requiredCookies) {
            String[] pair = cookie.split("=");
            if (pair[0].equals(MustitRequiredCookies.PHPSESSID.name())) {
                cookies.put(MustitRequiredCookies.PHPSESSID.name(), pair[1]);
            } else if (pair[0].equals(MustitRequiredCookies.AWSALB.name())) {
                cookies.put(MustitRequiredCookies.AWSALB.name(), pair[1]);
            } else if (pair[0].equals(MustitRequiredCookies.AWSALBCORS.name())) {
                cookies.put(MustitRequiredCookies.AWSALBCORS.name(), pair[1]);
            }
        }

        return cookies;
    }
}
