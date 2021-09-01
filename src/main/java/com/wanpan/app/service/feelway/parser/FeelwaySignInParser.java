package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.dto.feelway.FeelwaySignIn;
import com.wanpan.app.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public class FeelwaySignInParser {
    private static final String SESSION_PREFIX = "PHPSESSID=";

    public static boolean isKeepSignIn(String html, String accountId) {
        return html.contains(accountId);
    }

    public static void assertLoginSuccess(String html, FeelwaySignIn feelwaySignIn) {
        String successContainMessage = "alert('" + feelwaySignIn.getLoginId();
        if (!html.contains(successContainMessage)) {
            log.info("html:{}", html);
            Document document = Jsoup.parse(html);
            throw new InvalidRequestException(document.select("script").get(1).html().split("'")[1]);
        }
    }

    public static String getSession(String header) {
        if (header.contains(SESSION_PREFIX)) {
            return header.replace(SESSION_PREFIX, "").split(";")[0];
        }

        return null;
    }
}
