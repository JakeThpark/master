package com.wanpan.app.service.feelway.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.ShopNoticeDto;
import com.wanpan.app.service.feelway.util.FeelwayResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FeelwayNoticeParser {
    /**
     *
     * @param contentsHtml
     * @return
     */
    public static List<ShopNoticeDto> parseShopNotice(final String contentsHtml) {
        log.info("parseShopNotice CAll");
        List<ShopNoticeDto> shopNoticeDtoList = new ArrayList<>();

        Document document = Jsoup.parse(contentsHtml);
        Elements noticeListElements = document.select("table.notice-lists > tbody >tr");
        for(Element noticeElement : noticeListElements){
            Elements noticeElements = noticeElement.select("td");
            log.info("===============================================");
            Element subjectLinkElement =  noticeElements.get(0).getElementsByTag("a").first();
            String noticeId = PatternExtractor.FEELWAY_NOTICE_ID.extract(subjectLinkElement.attr("href"),1);
            String subject = subjectLinkElement.html();
            String writer = noticeElements.get(1).html();
            //20-10-26 15:48:35
            String createDate = noticeElements.get(2).html();

            ShopNoticeDto shopNoticeDto = new ShopNoticeDto();
            shopNoticeDto.setShopNoticeId(noticeId);
            shopNoticeDto.setSubject(subject);
            shopNoticeDto.setWriter(writer);
//            shopNoticeDto.setRegisteredDate(createDate);
            shopNoticeDto.setRegisteredDate(FeelwayResponseConverter.noticeTimeConvertToLocalDateTime(createDate));
            shopNoticeDtoList.add(shopNoticeDto);
            log.info("글번호:{}",noticeId);
            log.info("제목:{}",subject);
            log.info("작성자:{}",writer);
            log.info("등록일:{}", FeelwayResponseConverter.noticeTimeConvertToLocalDateTime(createDate));
        }
        log.info("===============================================");

        return shopNoticeDtoList;
    }

    public static String parseShopNoticeDetail(final String contentsHtml) {
        log.info("parseShopNoticeDetail CAll");
        Document document = Jsoup.parse(contentsHtml);
        //jsoup으로 파싱시에 tbody가 들어간다
        String noticeDetail = document.select("table.notice-lists > tbody > tr > td > table > tbody > tr > td").first().html();
        log.info(noticeDetail);
        log.info("===============================================");
        return noticeDetail;
    }
}
