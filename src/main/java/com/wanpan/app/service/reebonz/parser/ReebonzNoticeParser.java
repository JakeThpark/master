package com.wanpan.app.service.reebonz.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.ShopNoticeDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReebonzNoticeParser {
    public static List<ShopNoticeDto> parseShopNotice(final String contentsHtml) {
        log.info("parseShopNotice CAll");
        List<ShopNoticeDto> shopNoticeDtoList = new ArrayList<>();

        Document document = Jsoup.parse(contentsHtml);
        Elements noticeListElements = document.select("ul.notice-list > li.wrap-panel");
        for(Element noticeElement : noticeListElements){
            Element titleElement = noticeElement.select("div.wrap-col.panel-head").first();
            String noticeId = PatternExtractor.REEBONZ_NOTICE_ID.extract(titleElement.attr("id"),1);
            String subject = titleElement.select("div.col.col1").first().html();
            String createDate = titleElement.select("div.col.col2").first().html();
            String writer = "REEBONZ";
            String contents = noticeElement.select("div.panel-body > div.content").first().html();
            log.info("===============================================");
            ShopNoticeDto shopNoticeDto = new ShopNoticeDto();
            shopNoticeDto.setShopNoticeId(noticeId);
            shopNoticeDto.setSubject(subject);
            shopNoticeDto.setWriter(writer);
            shopNoticeDto.setRegisteredDate(noticeTimeConvertToLocalDateTime(createDate));
            shopNoticeDto.setContents(contents);
            shopNoticeDtoList.add(shopNoticeDto);
            log.info("글번호:{}",noticeId);
            log.info("제목:{}",subject);
            log.info("작성자:{}",writer);
            log.info("등록일:{}", createDate);
            log.info("내용:{}", contents);
        }
        log.info("===============================================");

        return shopNoticeDtoList;
    }

    public static LocalDateTime noticeTimeConvertToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr + " 00:00:00", formatter);
    }
}
