package com.wanpan.app.service.mustit.parser;

import com.wanpan.app.dto.job.order.OrderBaseConversationMessageDto;
import com.wanpan.app.dto.mustit.*;
import com.wanpan.app.service.mustit.constant.MustitOrderStatus;
import com.wanpan.app.service.mustit.constant.MustitOrderStatusGroupType;
import com.wanpan.app.service.mustit.constant.MustitSaleStatus;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MustitHtmlParser {
    private static final Pattern SALE_ID_PATTERN = Pattern.compile("(상품번호:)([0-9]+)");
    private static final Pattern ORDER_CONVERSATION_COMMENT_CREATION_DATE_PATTERN = Pattern.compile("[0-9]+년\\s[0-9]+월\\s[0-9]+일");
    private static final Pattern ORDER_CONVERSATION_COMMENT_CREATION_TIME_PATTERN = Pattern.compile("[0-9]+:[0-9]+");
    private static final Pattern ORDER_UNIQUE_ID_PATTERN_FROM_MEMO_WRITE_URL = Pattern.compile("(/)([0-9]+)");
    private static final Pattern RETURN_DELIVERY_INFO_PATTERN_FROM_RETURN_SEARCH_URL = Pattern.compile("(')(.*)(','')");
    private static final Pattern ALERT_MESSAGE_PATTERN = Pattern.compile("alert\\('(?<msg>.*)'\\)");

    public static boolean isKeepSignIn(String html, String accountId) {

        return html.contains(accountId + "님");
    }

    public static String getSaleId(String html){
        Matcher matcher = SALE_ID_PATTERN.matcher(html);
        if(matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    /**
     * 판매글 수정을 위해 기존값 정보가 필요한 항목들을 가져온다.
     */
    public static MustitSale getSale(String html) {
        MustitSale mustitSale = new MustitSale();
        Document document = Jsoup.parse(html);

        // 상품 이미지 정보 가져오기
        for (Element e : document.select("[name=imgSrc[]]")) {
            mustitSale.getImageList().add(
                    new MustitProductImage("0", e.val()));
        }

        return mustitSale;
    }

    /**
     * 주문대화 코멘트 목록을 가져온다.
     */
    public static List<OrderBaseConversationMessageDto> getOrderConversationList(String html) {
        List<OrderBaseConversationMessageDto> orderConversationList = new ArrayList<>();

        Document document = Jsoup.parse(html);
        Elements comments = document.select(".talk");

        if (comments.isEmpty()){
            return orderConversationList;
        }

        // 작성일자 요소를 찾으면 사용하고, 찾지 못하면 기존 작성일자 그대로 사용
        String creationDate = null;
        for (int i = 0; i < comments.size(); i++) {
            Element comment = comments.get(i);
            String foundCreationDate = getOrderConversationMessageCreationDate(comment);
            if (foundCreationDate != null) {
                creationDate = foundCreationDate;
            }

            LocalDateTime creationDateTime = getOrderConversationMessageCreationDateTime(comment, creationDate);
            OrderBaseConversationMessageDto orderConversation = createOrderConversationMessage(String.valueOf(i), comment, creationDateTime);

            // 주문대화 목록에 추가
            orderConversationList.add(orderConversation);
        }

        return orderConversationList;
    }

    /**
     * "진행주문관리>전체" 페이지로부터 총 주문 개수를 구한다
     */
    public static int getOngoingOrderCount(String html) {
        return Integer.parseInt(
                Jsoup.parse(html).getElementsByClass("all").first()
                        .getElementsByClass("num").first()
                        .getElementsByClass("fs20").first()
                        .text());
    }

    /**
     * "완료주문관리>전체" 페이지로부터 총 주문 개수를 구한다
     */
    public static int getCompleteOrderCount(String html) {
        Document document = Jsoup.parse(html);

        return Integer.parseInt(document.getElementsByClass("num c_mustit").first().text().trim());
    }

    /**
     * "완료주문관리>정산예정" 페이지로부터 총 주문 개수를 구한다
     */
    public static int getCalculationScheduleOrderCount(String html) {
        Document document = Jsoup.parse(html);
        Element calculationScheduleStatusElement = document.attr("style", "background-color:rgb(247,247,247)");
        Element calculationScheduleCountElement = calculationScheduleStatusElement.getElementsByClass("num").first();

        return Integer.parseInt(calculationScheduleCountElement.text().trim());
    }

    /**
     * 주문목록 페이지로부터 머스트잇 교체해야 할 주문정보를 수집한다
     */
    public static MustitOrderInfoForReplace collectOrderInfoForReplaceFromOrderListPage(String html, MustitOrderInfoForReplace mustitOrderInfoForReplace, MustitOrderStatusGroupType mustitOrderStatusGroupType) {
        Document document = Jsoup.parse(html);
        Map<String, String> orderIdMap = mustitOrderInfoForReplace.getOrderIdMap();
        Map<String, String> orderStatusMap = mustitOrderInfoForReplace.getOrderStatusMap();
        Map<String, MustitDeliveryInfo> deliveryInfoMap = mustitOrderInfoForReplace.getDeliveryInfoMap();
        Map<String, MustitDeliveryInfo> exchangeDeliveryInfoMap = mustitOrderInfoForReplace.getExchangeDeliveryInfoMap();
        Map<String, MustitDeliveryInfo> returnDeliveryInfoMap = mustitOrderInfoForReplace.getReturnDeliveryInfoMap();

        if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.ONGOING) { // "진행주문"
            Element ongoingOrderTableElement = document.getElementsByClass("order_progress").first();
            Elements rows = ongoingOrderTableElement.getElementsByTag("tr");
            for (Element row : rows) {
                Element orderUniqueIdElement = row.getElementsByClass("c_black under_btn").first();
                if (orderUniqueIdElement != null) {
                    String orderUniqueId = orderUniqueIdElement.attr("onclick").split("/")[3];

                    // 교체용 주문번호 수집
                    Element orderIdElement = row.getElementById("wizfasta");
                    if (orderIdElement != null) {
                        String orderIdForReplace = orderIdElement.text();
                        String[] splitOrderId = orderIdForReplace.split("-");
                        String orderId = splitOrderId[0];
                        if (!orderIdMap.containsKey(orderId)) {
                            orderIdMap.put(orderId, orderIdForReplace);
                        }
                    }

                    // 주문상태 수집
                    Element orderStatusElement = row.getElementsByClass("btn_size_change").first();
                    if (orderStatusElement != null) {
                        String orderStatus = orderStatusElement.getElementsByClass("mi-font-mblack bold mi-text-interval-basic").first().text().replace(" ", "");
                        if (!orderStatusMap.containsKey(orderUniqueId)) {
                            orderStatusMap.put(orderUniqueId, orderStatus);
                        }

                        switch (orderStatus) {
                            case "배송중":
                            case "반품요청":
                            case "교환요청":
                                // 배송정보 수집 대상 주문고유번호 저장
                                if (!deliveryInfoMap.containsKey(orderUniqueId)) {
                                    deliveryInfoMap.put(orderUniqueId, null);
                                }
                                break;
                            case "배송중(교환)":
                                // 배송정보 수집 대상 주문고유번호 저장
                                if (!deliveryInfoMap.containsKey(orderUniqueId)) {
                                    deliveryInfoMap.put(orderUniqueId, null);
                                }
                                // 교환배송정보 수집 대상 주문고유번호 저장
                                if (!exchangeDeliveryInfoMap.containsKey(orderUniqueId)) {
                                    exchangeDeliveryInfoMap.put(orderUniqueId, null);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    // 반품배송정보 수집
                    Element returnDeliveryInfoElement = row.getElementsByAttributeValue("value", "반송조회").first();
                    if (returnDeliveryInfoElement != null) {
                        if (!returnDeliveryInfoMap.containsKey(orderUniqueId)) {
                            returnDeliveryInfoMap.put(
                                    orderUniqueId,
                                    MustitHtmlParser.getReturnDeliveryInfo(returnDeliveryInfoElement));
                        }
                    }
                }
           }
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.CALCULATION_SCHEDULE) { // "정산예정 주문"
            Element calculationRequestOrderTableElement = document.getElementsByClass("new_mypage_table").last();
            Elements rows = calculationRequestOrderTableElement.getElementsByTag("tr");
            for (Element row : rows) {
                // 주문상태 수집
                Element orderUniqueIdElement = row.getElementById("wizfasta2");
                Element orderStatusElement = row.getElementsByAttributeValueContaining("class", "fs15 bold statText").first();
                if (orderStatusElement != null && orderUniqueIdElement != null) {
                    String orderUniqueId = orderUniqueIdElement.text().trim();
                    String orderStatus = orderStatusElement.text().replace(" ", "");
                    if (!orderStatusMap.containsKey(orderUniqueId)) {
                        orderStatusMap.put(orderUniqueId, orderStatus);
                    }
                }
            }
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.COMPLETE) { // "완료주문"
            Element completeOrderTable = document.getElementsByClass("new_mypage_table").last();
            if (completeOrderTable != null) {
                Elements rows = completeOrderTable.getElementsByTag("tr");
                for (Element row : rows) {
                    Element orderIdElement = row.getElementById("wizfasta");
                    if (orderIdElement != null) {
                        String orderIdForReplace = orderIdElement.text();
                        String[] splitOrderId = orderIdForReplace.split("-");
                        String orderId = splitOrderId[0];
                        if (!orderIdMap.containsKey(orderId)) {
                            orderIdMap.put(orderId, orderIdForReplace);
                        }
                    }
                }
            }
        }

        return mustitOrderInfoForReplace;
    }

    /**
     * 해당 주문건에 대한 현재 주문상태를 구한다
     */
    public static String getOrderStatus(String html, String orderUniqueId) {
        Document document = Jsoup.parse(html);
        Element orderDetailTable = document.getElementsByClass("new_mypage_table").first();
        Elements rows = orderDetailTable.getElementsByTag("tr");
        for (Element row : rows) {
            Element lastColumn = row.getElementsByTag("td").last();
            if (lastColumn != null) {
                Element memoWriteElement = lastColumn.getElementsByAttributeValue("value", "메모작성").first();
                if (memoWriteElement != null) {
                    Matcher matcher = ORDER_UNIQUE_ID_PATTERN_FROM_MEMO_WRITE_URL.matcher(memoWriteElement.attr("onclick"));
                    if (matcher.find()) {
                        String foundOrderUniqueId = matcher.group(2);
                        if (Objects.equals(foundOrderUniqueId, orderUniqueId)) {
                            StringBuilder orderStatus = new StringBuilder();
                            Elements orderStatusElements = lastColumn.getElementsByTag("span");
                            for (Element orderStatusElement : orderStatusElements) {
                                orderStatus.append(orderStatusElement.text().trim());
                            }
                            return orderStatus.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 배송정보를 구한다
     */
    public static MustitDeliveryInfo getDeliveryInfo(String html) {
        Document document = Jsoup.parse(html);
        MustitDeliveryInfo mustitDeliveryInfo = new MustitDeliveryInfo();
        Element orderDetailTable = document.getElementsByClass("common_table").first();
        Elements rows = orderDetailTable.getElementsByTag("tr");

        for (Element row : rows) {
            Element rowNameElement = row.getElementsByTag("th").first();
            if (rowNameElement != null) {
                String name = rowNameElement.text();
                switch (name) {
                    case "배송방식":
                        Element deliveryTypeElement = row.getElementsByTag("td").first();
                        mustitDeliveryInfo.setDeliveryType(deliveryTypeElement.text().trim());
                        break;
                    case "송장번호":
                        Elements deliveryInfoElements = row.getElementsByTag("span");
                        mustitDeliveryInfo.setCourierName(deliveryInfoElements.first().html().replace("\n","").replaceAll("</.*>",""));
                        mustitDeliveryInfo.setTrackingNumber(deliveryInfoElements.last().text().trim());
                        break;
                    default:
                        break;
                }
            }
        }

        return mustitDeliveryInfo;
    }

    /**
     * 반품정보를 구한다
     */
    public static MustitDeliveryInfo getReturnDeliveryInfo(Element element) {
        MustitDeliveryInfo mustitDeliveryInfo = new MustitDeliveryInfo();
        Matcher matcher = RETURN_DELIVERY_INFO_PATTERN_FROM_RETURN_SEARCH_URL.matcher(element.attr("onclick"));
        if (matcher.find()) {
            String returnInfoUrl = matcher.group(2);
            String[] returnInfo = returnInfoUrl.split("/");

            mustitDeliveryInfo.setCourierName(returnInfo[3]);
            mustitDeliveryInfo.setTrackingNumber(returnInfo[4]);

            return mustitDeliveryInfo;
        }

        return null;
    }

    /**
     * 주문대화 메세지 객체를 생성한다
     */
    private static OrderBaseConversationMessageDto createOrderConversationMessage(String messageId, Element element, LocalDateTime creationDateTime) {
        OrderBaseConversationMessageDto orderConversationMessage = new OrderBaseConversationMessageDto();

        // 메세지 아이디
        orderConversationMessage.setShopMessageId(messageId);

        // 작성일시
        orderConversationMessage.setPostAt(creationDateTime);

        // 타입
        Elements buyerIdElements = element.getElementsByAttributeValueContaining("style", "margin:0 10px;");
        if (!buyerIdElements.isEmpty()) {
            orderConversationMessage.setType(OrderBaseConversationMessageDto.Type.BUYER);
        } else {
            orderConversationMessage.setType(OrderBaseConversationMessageDto.Type.SELLER);
        }

        // 내용
        Elements contentElements = element.getElementsByClass("talk_content");
        if (!contentElements.isEmpty()) {
            orderConversationMessage.setContent(contentElements.first().text());
        }

        return orderConversationMessage;
    }

    /**
     * 주문대화 메세지 작성일시를 구한다
     */
    private static LocalDateTime getOrderConversationMessageCreationDateTime(Element element, String creationDate) {
        Elements questionTimeElements = element.getElementsByClass("mi-group-l10"); // 상대방의 대화 작성시간 찾기
        Elements answerTimeElements = element.getElementsByClass("mi-group-r10"); // 본인의 대화 작성시간 찾기

        Matcher creationTimeMatcher;
        if (!questionTimeElements.isEmpty()) {
            creationTimeMatcher = ORDER_CONVERSATION_COMMENT_CREATION_TIME_PATTERN.matcher(questionTimeElements.first().text());
        } else if (!answerTimeElements.isEmpty()) {
            creationTimeMatcher = ORDER_CONVERSATION_COMMENT_CREATION_TIME_PATTERN.matcher(answerTimeElements.first().text());
        } else {
            return null;
        }

        if (creationTimeMatcher.find()) {
            String creationTime = creationTimeMatcher.group();
            String creationDateTime = creationDate + " " + creationTime;

            return LocalDateTime.parse(creationDateTime, DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"));
        }

        return null;
    }

    /**
     * 주문대화 메세지 작성일자를 구한다
     */
    private static String getOrderConversationMessageCreationDate(Element element) {
        String creationDate = null;
        Elements dateElements = element.getElementsByClass("daily");

        if (!dateElements.isEmpty()) {
            Matcher creationDateMatcher = ORDER_CONVERSATION_COMMENT_CREATION_DATE_PATTERN.matcher(dateElements.first().text());
            if (creationDateMatcher.find()) {
                creationDate = creationDateMatcher.group();
            }
        }

        return creationDate;
    }

    /**
     * 주문 업데이트 작업이 성공했는지 확인한다.
     */
    public static boolean isUpdateOrder(String html, MustitOrderStatus currentStatus, List<MustitOrderStatus> targetStatusList) {
        boolean result = false;
        List<String> successMessageList = new ArrayList<>();

        if (targetStatusList.contains(currentStatus)) {
            result = true;
        } else {
            switch (currentStatus) {
                case PAYMENT_COMPLETE:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.DELIVERY_READY.equals(targetStatus)) {
                            // "배송준비중"으로 업데이트 성공메세지
                            successMessageList.add("상품발주 처리되었습니다");
                        } else if (MustitOrderStatus.DELIVERY.equals(targetStatus)) {
                            // "배송중"으로 업데이트 성공메세지
                            successMessageList.add("주문정보가 업데이트 되었습니다");
                        } else if (Arrays.asList(
                                MustitOrderStatus.SELL_CANCEL,
                                MustitOrderStatus.SELL_CANCEL_COMPLETE).contains(targetStatus)) {
                            // 판매취소 성공메세지
                            successMessageList.add("판매취소 처리 되었습니다");
                        }
                    }
                    break;
                case DELIVERY_READY:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.DELIVERY.equals(targetStatus)) {
                            // "배송중"으로 업데이트 성공메세지
                            successMessageList.add("주문정보가 업데이트 되었습니다");
                        } else if (Arrays.asList(
                                MustitOrderStatus.SELL_CANCEL,
                                MustitOrderStatus.SELL_CANCEL_COMPLETE).contains(targetStatus)) {
                            // 판매취소 성공메세지
                            successMessageList.add("판매취소 처리 되었습니다");
                        }
                    }
                    break;
                case PAYMENT_COMPLETE_BY_EXCHANGE:
                case DELIVERY_READY_BY_EXCHANGE:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.DELIVERY_BY_EXCHANGE.equals(targetStatus)) {
                            // "배송중(교환)"으로 업데이트 성공메시지
                            successMessageList.add("주문정보가 업데이트 되었습니다");
                        } else if (Arrays.asList(
                                MustitOrderStatus.SELL_CANCEL,
                                MustitOrderStatus.SELL_CANCEL_COMPLETE).contains(targetStatus)) {
                            // 판매취소 성공메세지
                            successMessageList.add("판매취소 처리 되었습니다");
                        }
                    }
                    break;
                case EXCHANGE_REQUEST:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.PAYMENT_COMPLETE_BY_EXCHANGE.equals(targetStatus)) {
                            // 교환승인 성공메세지
                            successMessageList.add("교환 상품도착을 확인하였습니다");
                        } else if (MustitOrderStatus.DELIVERY_BY_EXCHANGE.equals(targetStatus)) {
                            // 교환승인 성공메세지
                            successMessageList.add("주문정보가 업데이트 되었습니다");
                        } else if (MustitOrderStatus.DELIVERY.equals(targetStatus)) {
                            // 교환거절 성공메세지
                            successMessageList.add("반려가 승인 되었습니다");
                        }
                    }
                    break;
                case RETURN_REQUEST:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (Arrays.asList(
                                MustitOrderStatus.RETURN_CONFIRM,
                                MustitOrderStatus.RETURN_COMPLETE).contains(targetStatus)) {
                            // 반품승인 성공메세지
                            successMessageList.add("반품 상품도착을 확인하였습니다");
                        } else if (MustitOrderStatus.DELIVERY.equals(targetStatus)) {
                            // 반품거절 성공메세지
                            successMessageList.add("반려가 승인 되었습니다");
                        }
                    }
                    break;
                case BUY_CANCEL_REQUEST:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.BUY_CANCEL_COMPLETE.equals(targetStatus)) {
                            // 구매취소승인 성공메세지
                            successMessageList.add("구매취소가 수락되었습니다");
                        } else if (Arrays.asList(
                                MustitOrderStatus.DELIVERY,
                                MustitOrderStatus.DELIVERY_BY_EXCHANGE).contains(targetStatus)) {
                            // 구매취소거절 성공메세지
                            successMessageList.add("반려가 승인 되었습니다");
                        }
                    }
                case CALCULATION_SCHEDULE:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.CALCULATION_DELAY.equals(targetStatus)) {
                            // "정산보류중"으로 업데이트 성공메세지
                            successMessageList.add("Y");
                            break;
                        }
                    }
                    break;
                case CALCULATION_DELAY:
                    for (MustitOrderStatus targetStatus : targetStatusList) {
                        if (MustitOrderStatus.CALCULATION_SCHEDULE.equals(targetStatus)) {
                            // "정산예정"으로 업데이트 성공메세지
                            successMessageList.add("N");
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }

            for (String successMessage : successMessageList) {
                if (html.contains(successMessage)) {
                    result = true;
                    break;
                }
            }

        }

        return result;
    }

    /**
     * alert 메시지를 구한다
     */
    public static String getAlertMessage(String html) {
        Matcher matcher = ALERT_MESSAGE_PATTERN.matcher(html);

        if (matcher.find()) {
            return matcher.group("msg");
        }

        return null;
    }

    /**
     * 주문회원 정보를 구한다
     */
    public static MustitOrderBuyer getMustitOrderBuyer(String html) {
        MustitOrderBuyer mustitOrderBuyer = new MustitOrderBuyer();

        Document document = Jsoup.parse(html);
        Element buyerTable = document.getElementsByClass("nuts_table information").first();
        Elements rows = buyerTable.getElementsByTag("tr");

        for (Element row : rows) {
            String itemName = row.getElementsByTag("th").first().text().trim();
            String itemValue = row.getElementsByTag("td").first().text().trim();

            switch (itemName) {
                case "구매자 이름":
                    mustitOrderBuyer.setName(itemValue);
                    break;
                case "구매자 메일":
                    mustitOrderBuyer.setEmail(itemValue);
                    break;
                case "구매자 전화":
                    mustitOrderBuyer.setPhoneNumber(itemValue);
                    break;
                case "구매자 핸드폰":
                    mustitOrderBuyer.setMobilePhoneNumber(itemValue);
                    break;
                default:
                    break;
            }
        }

        return mustitOrderBuyer;
    }

    /**
     * 품절상품관리 페이지로부터 판매글 상태를 구한다
     */
    public static MustitSaleStatus parseMustitSaleStatusBySoldOutPage(String html) {
        Document document = Jsoup.parse(html);
        Element soldOutSaleListTableElement = document.getElementsByClass("new_mypage_table").first();
        Element saleStatusElement = soldOutSaleListTableElement.getElementsByTag("b").first();

        return saleStatusElement != null ? MustitSaleStatus.getByCode(saleStatusElement.text().trim()) : null;
    }

    /**
     * 판매상품관리 페이지로부터 판매글 상태를 구한다
     */
    public static MustitSaleStatus getMustitSaleStatusBySellingPage(String html) {
        // 판매상품관리 페이지에 조회된 데이터가 있으면 "ON_SALE"
        Document document = Jsoup.parse(html);
        Element saleIdElement = document.select(".new_mypage_table > tbody > tr > td > p").last();
        return saleIdElement != null ? MustitSaleStatus.ON_SALE : null;
    }
}
