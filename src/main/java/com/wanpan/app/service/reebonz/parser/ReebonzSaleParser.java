package com.wanpan.app.service.reebonz.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReebonzSaleParser {
    private static Pattern uploadedImageUrlPattern = Pattern.compile("'(?<imgUrl>.*)'\\)");

    /**
     * 리본즈 판매글 상세설명 첨부용 이미지 URL을 파싱한다
     */
    public String parseUploadedImageUrl(String htmlContents) {
        Matcher matcher = uploadedImageUrlPattern.matcher(htmlContents);

        if (matcher.find()) {
            return matcher.group("imgUrl").trim();
        }

        return null;
    }
}
