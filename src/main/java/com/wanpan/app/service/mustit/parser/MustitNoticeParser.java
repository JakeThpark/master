package com.wanpan.app.service.mustit.parser;

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
public class MustitNoticeParser {
    public static List<ShopNoticeDto> parseShopNotice(final String contentsHtml) {
        log.info("parseShopNotice CAll");
        List<ShopNoticeDto> shopNoticeDtoList = new ArrayList<>();

        Document document = Jsoup.parse(contentsHtml);
        Elements noticeListElements = document.select("table.list_table > tbody >tr");
        //첫 tr라인을 확인하기 위함
        boolean isContents = false;
        for(Element noticeElement : noticeListElements){
            if(!isContents){ //첫 tr라인은 title 행이다
                isContents = true;
                continue;
            }

            //머스트잇 화면에 보이는 번호랑 실제 상세 링크의 ID가 다르다. 상세를 위해 링크에 걸린 번호를 추출한다.
            String noticeId = PatternExtractor.MUSTIT_NOTICE_ID.extract(noticeElement.attr("onclick"),1);
            Elements noticeElements = noticeElement.select("td");
            log.info("===============================================");
            String subject = PatternExtractor.removeTag(noticeElements.get(1).html());
            String writer = noticeElements.get(2).html();
            String createDate = noticeElements.get(3).html(); //YYYY-MM-DD
            ShopNoticeDto shopNoticeDto = new ShopNoticeDto();
            shopNoticeDto.setShopNoticeId(noticeId);
            shopNoticeDto.setSubject(subject);
            shopNoticeDto.setWriter(writer);
            shopNoticeDto.setRegisteredDate(noticeTimeConvertToLocalDateTime(createDate));
            shopNoticeDtoList.add(shopNoticeDto);
            log.info("글번호:{}",noticeId);
            log.info("제목:{}",subject);
            log.info("작성자:{}",writer);
            log.info("등록일:{}", createDate);
        }
        log.info("===============================================");

        return shopNoticeDtoList;
    }

    public static String parseShopNoticeDetail(final String contentsHtml) {
        log.info("parseShopNoticeDetail CAll");
        Document document = Jsoup.parse(contentsHtml);
        //jsoup으로 파싱시에 tbody가 들어간다
        String noticeDetail = document.select("div.nanumEditor").first().html();
        log.info(noticeDetail);
        log.info("===============================================");
        return noticeDetail;
    }

    public static LocalDateTime noticeTimeConvertToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr + " 00:00:00", formatter);
    }
}
