package com.wanpan.app.service.reebonz.parser;

import com.wanpan.app.dto.job.order.OrderBaseConversationDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationMessageDto;
import com.wanpan.app.dto.reebonz.QnaStatus;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReebonzOrderConversationParser {
    private static final Pattern ORDER_CONVERSATION_MESSAGE_WRITER_PATTERN = Pattern.compile("\\((?<writer>.*)\\)");
    private static final Pattern ORDER_CONVERSATION_MESSAGE_ID_PATTERN = Pattern.compile("(qna_)(?<messageId>.*)");

    /**
     * 주문대화 목록 Page html을 파싱한다.
     */
    public List<OrderBaseConversationDto> parseOrderConversation(String htmlContents) {
        List<OrderBaseConversationDto> orderBaseConversationList = new ArrayList<>();
        Document document = Jsoup.parse(htmlContents);

        Elements rows =  document.getElementsByClass("table-row");
        if (!rows.isEmpty()) {
            for (Element row : rows) {
                Elements cells = row.getElementsByClass("table-cell");
                if (!cells.isEmpty()) {
                    OrderBaseConversationDto orderBaseConversation = new OrderBaseConversationDto();
                    orderBaseConversation.setChannelId(cells.get(0).text().trim());
                    orderBaseConversationList.add(orderBaseConversation);
                }
            }
        }

        return orderBaseConversationList;
    }

    /**
     * 주문대화 상세 Page html을 파싱한다.
     */
    public OrderBaseConversationDto parseOrderBaseConversation(String htmlContents, String qnaId) {
        OrderBaseConversationDto orderBaseConversationDto = new OrderBaseConversationDto();

        // 주문번호
        orderBaseConversationDto.setOrderId(
                getOrderConversationOrderId(htmlContents));

        // 주문유니크번호
        orderBaseConversationDto.setOrderUniqueId(
                getOrderConversationOrderUniqueId(htmlContents));

        // 메세지 목록
        orderBaseConversationDto.setOrderBaseConversationMessageList(
                getOrderConversationMessages(htmlContents, qnaId));

        return orderBaseConversationDto;
    }

    /**
     * 주문대화 메세지 목록 Page html을 파싱한다.
     */
    public List<OrderBaseConversationMessageDto> getOrderConversationMessages(String htmlContents, String qnaId) {
        List<OrderBaseConversationMessageDto> orderBaseConversationMessageList = new ArrayList<>();
        Document document = Jsoup.parse(htmlContents);
        Element qnaContentElement = document.getElementsByClass("wrap-qns-content").first();
        if (qnaContentElement != null) {
            Elements rows = qnaContentElement.getElementsByClass("table-row");
            if (!rows.isEmpty()) {
                for (Element row : rows) {
                    OrderBaseConversationMessageDto orderConversationMessage = new OrderBaseConversationMessageDto();

                    // 타입 및 작성날짜 세팅
                    Element writeTimeElement = row.getElementsByClass("time").first();
                    if (writeTimeElement != null) {
                        String writeTimeAndWriter = writeTimeElement.text().trim();
                        String writer = parseWriterOfOrderConversationMessage(writeTimeAndWriter);

                        // 타입 세팅
                        if (writer != null) {
                            String partnerName = parsePartnerName(htmlContents);
                            if (writer.equals(partnerName)) {
                                orderConversationMessage.setType(OrderBaseConversationMessageDto.Type.SELLER);
                            } else {
                                orderConversationMessage.setType(OrderBaseConversationMessageDto.Type.SHOP);
                            }
                        } else {
                            orderConversationMessage.setType(OrderBaseConversationMessageDto.Type.BUYER);
                        }

                        // 작성날짜 세팅
                        String writeTime = writeTimeAndWriter.replace(String.format("(%s)", writer), "").trim();
                        orderConversationMessage.setPostAt(
                                LocalDateTime.parse(writeTime, DateTimeFormatter.ofPattern("yyyy. MM. dd HH:mm:ss")));
                    }

                    // 내용 세팅
                    Element contentElement = row.getElementsByClass("qna-content").first();
                    if (contentElement != null) {
                        orderConversationMessage.setContent(contentElement.text().trim());
                    }


                    // 메세지 ID 세팅
                    Element qnaTitleElement = row.getElementsByClass("qna-title").first();
                    if (qnaTitleElement != null) {
                        orderConversationMessage.setShopMessageId(qnaId);
                    } else {
                        Element cellElement = row.getElementsByClass("table-cell").first();
                        if (cellElement != null) {
                            String cellId = cellElement.id();
                            orderConversationMessage.setShopMessageId(
                                    parseOrderConversationMessageId(cellId));
                        }
                    }

                    orderBaseConversationMessageList.add(orderConversationMessage);
                }
            }
        }

        return orderBaseConversationMessageList;
    }

    private String parsePartnerName(String htmlContents) {
        Document document = Jsoup.parse(htmlContents);
        Element partnerNameElement = document.getElementsByClass("partner-name").first();

        if (partnerNameElement != null) {
            return partnerNameElement.text().replace("\"", "").trim();
        }

        return null;
    }

    /**
     * 주문대화 메세지 작성자를 구한다.
     */
    private String parseWriterOfOrderConversationMessage(String htmlContents) {
        Matcher matcher = ORDER_CONVERSATION_MESSAGE_WRITER_PATTERN.matcher(htmlContents);

        if (matcher.find()) {
            return matcher.group("writer").trim();
        }

        return null;
    }

    /**
     * 주문대화 메세지 ID를 구한다.
     */
    private String parseOrderConversationMessageId(String htmlContents) {
        Matcher matcher = ORDER_CONVERSATION_MESSAGE_ID_PATTERN.matcher(htmlContents);

        if (matcher.find()) {
            return matcher.group("messageId").trim();
        }

        return null;
    }


    /**
     * 주문대화의 주문번호를 구한다.
     */
    private String getOrderConversationOrderId(String htmlContents) {
        String orderId = null;
        Document document = Jsoup.parse(htmlContents);
        Element orderIdElement = document.getElementsByClass("order-num").first();

        if (orderIdElement != null) {
            orderId = orderIdElement.text().trim();
        }

        return orderId;
    }

    /**
     * 주문대화의 주문유니크번호를 구한다.
     */
    private String getOrderConversationOrderUniqueId(String htmlContents) {
        String orderUniqueId = null;
        Document document = Jsoup.parse(htmlContents);
        Elements linkElements = document.getElementsByTag("a");

        for (Element linkElement : linkElements) {
            if ("주문정보".equals(linkElement.text())) {
                String[] splitHref = linkElement.attr("href").split("/");
                orderUniqueId = splitHref[splitHref.length - 1];
            }
        }

        return orderUniqueId;
    }

    /**
     * 해당 문의상태 관련 주문대화 총 건수를 구한다.
     */
    public int parseOrderConversationCountByQnAStatus(String htmlContents, QnaStatus qnAStatus) {
        Document document = Jsoup.parse(htmlContents);

        Element statusElement = document.getElementById(qnAStatus.getParamCode());
        if (statusElement != null) {
            Element countElement = statusElement.getElementsByClass("tab-count").first();
            if (countElement != null) {
                return Integer.parseInt(countElement.text());
            }
        }

        return 0;
    }

}
