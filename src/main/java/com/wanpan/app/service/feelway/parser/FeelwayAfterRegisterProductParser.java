package com.wanpan.app.service.feelway.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeelwayAfterRegisterProductParser {
    private static final String PRODUCT_ID_GROUP = "productId";
    private static final Pattern REGISTERED_PRODUCT_ID_PATTERN =
            Pattern.compile("view_goods\\.php\\?.+g_no=(?<" + PRODUCT_ID_GROUP + ">[0-9]+)");

    public static String getProductId(String html){
        Matcher matcher = REGISTERED_PRODUCT_ID_PATTERN.matcher(html);
        if(matcher.find()) {
            return matcher.group(PRODUCT_ID_GROUP);
        }
        return null;
    }
}
