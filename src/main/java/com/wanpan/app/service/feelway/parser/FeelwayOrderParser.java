package com.wanpan.app.service.feelway.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.job.order.OrderBaseConversationDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationMessageDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.service.feelway.util.FeelwayResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FeelwayOrderParser {
    private static String DELIVERY_FEE_GROUP_NAME = "deliveryFee";
    private static String DELIVERY_TYPE_GROUP_NAME = "deliveryMethod";
    private static String DELIVERY_FEE_TYPE_GROUP_NAME = "deliveryFeeType";

    private static Pattern deliveryFeePattern = Pattern.compile("예상비용 : (?<" + DELIVERY_FEE_GROUP_NAME + ">.*)원");
    private static Pattern deliveryTypePattern = Pattern.compile("배송방법 : (?<" + DELIVERY_TYPE_GROUP_NAME + ">.[^ ])");
    private static Pattern deliveryFeeTypePattern = Pattern.compile("배송비 : (?<" + DELIVERY_FEE_TYPE_GROUP_NAME + ">.*)");

    private static final String DIRECT_INPUT_COURIER_CODE = "ETC";

    /**
     * 주문 목록 페이지에 대해서 페이징 갯수를 읽어온다.
     * @param contentsHtml
     * @return
     */
    public static int getOrderListPaging(final String contentsHtml) {
        log.info("getOrderListPaging CAll");
        Document document = Jsoup.parse(contentsHtml);
        Elements elements = document.select("table.no");
        log.info("page table : {}",elements);
        return elements.first().select("tbody > tr > td > a").size();
    }

    public static List<OrderDto.Request.CollectCallback> parseOrderList(final String contentsHtml) {
//        log.info("contentsHtml:{}",contentsHtml);
        List<OrderDto.Request.CollectCallback> collectCallbackList = new ArrayList<>();

        try {
            log.info("parseOrderList CAll");
            Document document = Jsoup.parse(contentsHtml);
            Elements orderElements = document.getElementsByClass("link2");

            //각 주문단위별 Loop
            for (Element orderElement : orderElements) {
                Element trElement = orderElement.getElementsByTag("tbody").get(0);

                //OrderNumber, ProductId
                Map<Integer,String> orderNumberMap = PatternExtractor.FEELWAY_ORDER_PRODUCT_NUMBER.extractGroups(
                        trElement.getElementsMatchingText("\\s*주문번호\\s").get(1).getElementsByTag("td").get(1).html()
                );
                String orderNumber = orderNumberMap.get(1);
                String productNumber = orderNumberMap.get(2);

                //status, date
                Map<Integer,String> statusMap = PatternExtractor.FEELWAY_ORDER_STATUS_DATE.extractGroups(
                        trElement.getElementsMatchingText("\\s*처리상태\\s").get(1).getElementsByTag("td").get(1).html()
                );
                String status = statusMap.get(1);
                String statusDate = statusMap.get(2);

                //BRAND, ProductName
                Map<Integer,String> brandMap = PatternExtractor.FEELWAY_ORDER_BRAND_PRODUCT_NAME.extractGroups(
                        trElement.getElementsMatchingText("\\s*상품\\s").get(1).getElementsByTag("td").get(1).html()
                );
                String brandName = brandMap.get(1).trim();
                String productName = brandMap.get(2).trim();

                //price
                Map<Integer,String> priceMap = PatternExtractor.FEELWAY_ORDER_PRICE.extractGroups(
                        trElement.getElementsMatchingText("\\s*결제금액\\s").get(1).getElementsByTag("td").get(1).html()
                );
                Long price = Long.parseLong(priceMap.get(1).replace(",",""));

                //buyer, phone
                Element buyerTdElement = trElement.getElementsMatchingText("\\s*구매자\\/연락처\\s").get(1).getElementsByTag("td").get(1);
                Map<Integer,String> buyerMap = PatternExtractor.FEELWAY_ORDER_BUYER.extractGroups(buyerTdElement.html());
                String buyerName = buyerMap.get(1);
                String buyerId = buyerMap.get(2);
                Map<Integer,String> buyerPhoneMap = PatternExtractor.FEELWAY_ORDER_BUYER_PHONE.extractGroups(buyerTdElement.html());
                String phone = buyerPhoneMap.get(1);
                String mobile = buyerPhoneMap.get(2);

                //배송지 주소(필드는 존재하지만 내용이 없는 경우가 있음)
                String zipCode = null;
                String address = "";
                Elements deliveryElements = trElement.getElementsMatchingText("\\s*배송지\\s*주소\\s");
                if(deliveryElements.size() > 1
                        && PatternExtractor.FEELWAY_ORDER_TITLE_ADDRESS.extract(
                        deliveryElements.get(1).getElementsByTag("td").get(0).html()) != null
                ){

                    Map<Integer,String> deliveryMap = PatternExtractor.FEELWAY_ORDER_DELIVERY_ADDRESS.extractGroups(
                            deliveryElements.get(1).getElementsByTag("td").get(1).html()
                    );
                    zipCode = deliveryMap.get(1);
                    address = deliveryMap.get(2);
                }


                //요구사항/기타알림
                //사이즈/컬러
                //요구사항
                String memo = null;
                String optionName = null;
                String requirement = null;
                Elements requirementTdElements = trElement.getElementsMatchingText("\\s*요구사항\\/기타알림\\s");
                if(requirementTdElements.size() > 1){
                    memo = requirementTdElements.get(1).getElementsByTag("td").get(1).html();
                    String [] optionAndRequirement = memo.split("<br>");
                    if(!StringUtils.isEmpty(optionAndRequirement[0])){
                        optionName = PatternExtractor.FEELWAY_ORDER_OPTION.extractDefaultValue(optionAndRequirement[0],1, null);
                    }
                    if(optionAndRequirement.length > 1 && !StringUtils.isEmpty(optionAndRequirement[1])){
                        requirement = PatternExtractor.FEELWAY_ORDER_REQUIREMENT.extractDefaultValue(optionAndRequirement[1],1, null);
                    }
                }

                //배송방법(예정)
                String beforeDeliveryFeeType = null;
                String beforeDeliveryFee = null;
                String beforeDeliveryType = null;
                Elements beforeDeliveryFeeTypeElements = trElement.getElementsMatchingText("\\s*배송방법\\(예정\\)\\s");
                if(beforeDeliveryFeeTypeElements.size() > 1){
                    Element deliveryFeeTypeTdElement = beforeDeliveryFeeTypeElements.get(1).getElementsByTag("td").get(1);
                    String deliveryMessage = deliveryFeeTypeTdElement.html();

                    Matcher deliveryFeeTypeMatcher = deliveryFeeTypePattern.matcher(deliveryMessage);
                    if (deliveryFeeTypeMatcher.find()) {
                        beforeDeliveryFeeType = deliveryFeeTypeMatcher.group(DELIVERY_FEE_TYPE_GROUP_NAME);
                    }

                    Matcher deliveryTypeMatcher = deliveryTypePattern.matcher(deliveryMessage);
                    if (deliveryTypeMatcher.find()) {
                        beforeDeliveryType = deliveryTypeMatcher.group(DELIVERY_TYPE_GROUP_NAME);
                    }

                    Matcher deliveryFeeMatcher = deliveryFeePattern.matcher(deliveryMessage);
                    if (deliveryFeeMatcher.find()) {
                        beforeDeliveryFee = deliveryFeeMatcher.group(DELIVERY_FEE_GROUP_NAME);
                    }
                }

                //배송정보(실제)-배송중일때 , 배송정보 - 반송중일때
                String courierCode = null;
                String courierName = null;
                String courierCustomName = null;
                String trackingNumber = null;
                String deliverySellerMemo = null;
                Elements deliveryFeeTypeElements = trElement.getElementsMatchingText("\\s*배송정보\\(실제\\)\\s|\\s*배송정보\\s");
                if(deliveryFeeTypeElements.size() > 1
                        && PatternExtractor.FEELWAY_ORDER_TITLE_COURIER.extract(
                                deliveryFeeTypeElements.get(1).getElementsByTag("td").get(0).html()) != null
                ){
                    Element courierTdElement = deliveryFeeTypeElements.get(1).getElementsByTag("td").get(1);
                    Map<Integer,String> deliveryFeeTypeMap;
                    //태그안에 a 태그를 가지고 있지 않을 경우 직접입력으로 간주한다.
                    log.info("courierTdElement.html():{}",courierTdElement.html());
                    if(courierTdElement.getElementsByTag("a").size() == 0){
                        //message를 가지고 있는 내용 파싱
                        deliveryFeeTypeMap = PatternExtractor.FEELWAY_ORDER_DELIVERY_INFO_DIRECT_INPUT.extractGroups(courierTdElement.html());
                        //직접입력 case처리 고려해야함 - ETC를 사용
                        courierCode = DIRECT_INPUT_COURIER_CODE;
                    }else{
                        //a태그를 가지고 있는 html text 파싱
                        deliveryFeeTypeMap = PatternExtractor.FEELWAY_ORDER_DELIVERY_INFO_REAL.extractGroups(courierTdElement.html());
                        courierCode = deliveryFeeTypeMap.get(1) != null ? deliveryFeeTypeMap.get(1).trim() : null;
                    }
                    //courierCode가 null인 경우 택배사만 들어가서 업데이트가 안된경우라 볼수 있다.
                    if(courierCode != null){
                        //필웨이의 경우 택배사 코드랑 이름이 일치한다.
                        if (courierCode.equals(DIRECT_INPUT_COURIER_CODE)) {
                            courierCustomName = deliveryFeeTypeMap.get(1) != null ? deliveryFeeTypeMap.get(1).trim() : null;
                        } else {
                            courierName = deliveryFeeTypeMap.get(1) != null ? deliveryFeeTypeMap.get(1).trim() : null;
                        }
                        trackingNumber = deliveryFeeTypeMap.get(2) != null ? deliveryFeeTypeMap.get(2).trim() : null;
                        deliverySellerMemo = deliveryFeeTypeMap.get(3) != null ? deliveryFeeTypeMap.get(3).trim() : null;
                    }
                }

                //반품이유
                String returnReason = null;
                Elements returnReasonElements = trElement.getElementsMatchingText("\\s*반품사유\\s");
                if(returnReasonElements.size() > 1){
                    returnReason = returnReasonElements.get(1).getElementsByTag("td").get(1).html();
                }

                //판매취소 사유
                String saleCancellationReason = null;
                Elements saleCancellationReasonElements = trElement.getElementsMatchingText("\\s*판매취소\\s*사유\\s");
                if(saleCancellationReasonElements.size() > 1){
                    saleCancellationReason = saleCancellationReasonElements.get(1).getElementsByTag("td").get(1).html();
                }

                //구매취소 사유
                String purchaseCancellationReason = null;
                Elements purchaseCancellationReasonElements = trElement.getElementsMatchingText("\\s*구매취소\\s*사유\\s");
                if(purchaseCancellationReasonElements.size() > 1){
                    purchaseCancellationReason = purchaseCancellationReasonElements.get(1).getElementsByTag("td").get(1).html();
                }

                //반송정보
                String returnCourierCode = null;
                String returnCourierName = null;
                String returnTrackingNumber = null;
                Elements returnDeliveryElements = trElement.getElementsMatchingText("\\s*반송정보\\s");
                if(returnDeliveryElements.size() > 1){
                    Element returnDeliveryElement = returnDeliveryElements.get(1).getElementsByTag("td").get(1);
                    log.info("{}",returnDeliveryElement);
                    Map<Integer,String> returnDeliveryMap;
                    //태그안에 a 태그를 가지고 있지 않을 경우 직접입력으로 간주한다.
                    if(returnDeliveryElement.getElementsByTag("a").size() == 0){
                        //message를 가지고 있는 내용 파싱
                        returnDeliveryMap = PatternExtractor.FEELWAY_ORDER_DELIVERY_INFO_DIRECT_INPUT.extractGroups(returnDeliveryElement.html());
                        //직접입력 case처리 고려해야함 - ETC를 사용
                        returnCourierCode = DIRECT_INPUT_COURIER_CODE;
                    }else{
                        //a태그를 가지고 있는 html text 파싱
                        returnDeliveryMap = PatternExtractor.FEELWAY_ORDER_RETURN_DELIVERY.extractGroups(returnDeliveryElement.html());
                        returnCourierCode = returnDeliveryMap.get(1) != null ? returnDeliveryMap.get(1).trim() : null;
                    }
                    returnCourierName = returnDeliveryMap.get(1) != null ? returnDeliveryMap.get(1).trim() : null;
                    returnTrackingNumber = returnDeliveryMap.get(2) != null ? returnDeliveryMap.get(2).trim() : null;
                }

                //구매만족도
                String satisfaction = null;
                String sellerReply = null;
                Elements voteElements = trElement.getElementsMatchingText("\\s*구매만족도\\s");
                if(voteElements.size() > 1){
                    Map<Integer,String> voteMap = PatternExtractor.FEELWAY_ORDER_BUYER_VOTE.extractGroups(
                            voteElements.get(1).getElementsByTag("td").get(1).select(" > table > tbody > tr > td").get(0).html()
                    );
                    satisfaction = voteMap.get(1)+voteMap.get(2);
                    sellerReply = voteMap.get(3)+voteMap.get(4);
                }

                //정산지급 예정일
                String calculateDateAndMessage = null;
                Elements calculateDateElements = trElement.getElementsMatchingText("\\s*정산지급\\s*예정일\\s");
                if(calculateDateElements.size() > 1){
                    Map<Integer,String> calculateDateMap = PatternExtractor.FEELWAY_ORDER_CALCULATE_DATE.extractGroups(
                            calculateDateElements.get(1).getElementsByTag("td").get(1).html()
                    );
                    calculateDateAndMessage = calculateDateMap.get(1)+calculateDateMap.get(2);
                }

                //정산(예정)금액
                String calculateAmount = null;
                Elements calculateAmountElements = trElement.getElementsMatchingText("\\s*정산\\(예정\\)금액\\s");
                if(calculateAmountElements.size() > 1){
                    Map<Integer,String> calculateAmountMap = PatternExtractor.FEELWAY_ORDER_CALCULATE_AMOUNT.extractGroups(
                            calculateAmountElements.get(1).getElementsByTag("td").get(1).html()
                    );
                    calculateAmount = calculateAmountMap.get(1).replace(",","");
                }

                //처리방법
                String processMethod = null;
                Elements processMethodElements = trElement.getElementsMatchingText("\\s*처리방법\\s");
                if(processMethodElements.size() > 1){
                    processMethod = PatternExtractor.removeTag(processMethodElements.get(1).getElementsByTag("td").get(1).html());
                }

                //처리일시-절차(주문일이 기록되어 있음 - 무조건 존재)
                String processHistory = PatternExtractor.removeTag(
                        trElement.getElementsMatchingText("\\s*처리일시-절차\\s").get(1).getElementsByTag("td").get(1).html()
                );
                //월/일, 월-일 두가지 형식이 존재
                LocalDateTime orderDateTime = FeelwayResponseConverter.timeConvertToLocalDateTime(
                        PatternExtractor.FEELWAY_ORDER_DATE.extract(processHistory,1).trim().replace("/","-")
                );
                //판매자메모
                String sellerMemo = null;
                Elements sellerMemoElements = trElement.getElementsMatchingText("\\s*판매자메모\\s");
                if(sellerMemoElements.size() > 1){
                    sellerMemo = PatternExtractor.removeTag(PatternExtractor.removeTag(sellerMemoElements.get(1).getElementsByTag("td").get(1).html()));
                    sellerMemo = sellerMemo.replace("메모는 판매자 본인만 볼수 있으며, 구매자는 볼수 없습니다","").trim();
                }

                //구매자와 대화
                Elements buyerConversationElements = orderElement.getElementsMatchingText("\\s*구매자와\\s*대화\\s");
                List<OrderBaseConversationMessageDto> orderBaseConversationMessageDtoList = new ArrayList<>();
                if(buyerConversationElements.size() > 1){
                    Elements conversationTrElements = buyerConversationElements.get(2).getElementsByTag("td").get(1).select(" > table > tbody > tr");
                    //복수개의 대화시에 대화 사이에 tr이 존재한다. 해당 TR의 TD는 빈값이므로 이 값을 무시하고 처리한다. 최종적으로 두개의 TR은 입력과 관련한 태그이므로 무시한다.
                    int maxSize = conversationTrElements.size();
                    if(maxSize > 2){
                        int conversationId = 1;
                        for(int i = 0 ; i < maxSize - 2 ; i++){
                            String conversationTd = conversationTrElements.get(i).getElementsByTag("td").get(0).html();
                            if(StringUtils.isEmpty(conversationTd))
                                continue;
                            OrderBaseConversationMessageDto orderBaseConversationMessageDto = new OrderBaseConversationMessageDto();
                            //메세지 영역만 때어낸다.
                            Map<Integer,String> conversationMap = PatternExtractor.FEELWAY_ORDER_CONVERSATION_INFO_ID.extractGroups(conversationTd);
                            if("구매자".equals(conversationMap.get(1))){
                                orderBaseConversationMessageDto.setType(OrderBaseConversationMessageDto.Type.BUYER);
                            }else{
                                orderBaseConversationMessageDto.setType(OrderBaseConversationMessageDto.Type.SELLER);
                            }
                            orderBaseConversationMessageDto.setContent(conversationMap.get(2));
                            orderBaseConversationMessageDto.setPostAt(FeelwayResponseConverter.convertStrToLocalDateTime(conversationMap.get(4)));
//                            orderBaseConversationMessageDto.setWriterId(conversationMap.get(5));
                            orderBaseConversationMessageDto.setShopMessageId(String.valueOf(conversationId));
                            conversationId++;
                            orderBaseConversationMessageDtoList.add(orderBaseConversationMessageDto);

                            //기존 사용 로직 임시 보관
//                            orderBaseConversationMessageDto.setContent(PatternExtractor.removeTag(conversationTd));
//                            if(PatternExtractor.removeTag(conversationTd).contains("[구매자]")){
//                                orderBaseConversationMessageDto.setType(OrderBaseConversationMessageDto.Type.BUYER);
//                            }else{
//                                orderBaseConversationMessageDto.setType(OrderBaseConversationMessageDto.Type.SELLER);
//                            }
//                            Map<Integer,String> conversationMap = PatternExtractor.FEELWAY_ORDER_CONVERSATION_DATE_ID.extractGroups(conversationTd);
//                            log.info("conversationMap.get(1): {}",conversationMap.get(1));
//                            orderBaseConversationMessageDto.setPostAt(FeelwayResponseConverter.convertStrToLocalDateTime(conversationMap.get(1)));
//                            orderBaseConversationMessageDto.setWriterId(conversationMap.get(2));
//                            orderBaseConversationMessageDto.setShopMessageId(String.valueOf(conversationId));
//                            conversationId++;
//                            orderBaseConversationMessageDtoList.add(orderBaseConversationMessageDto);
                        }
                    }
                }
                OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                orderDto.setOrderId(orderNumber);
                orderDto.setOrderUniqueId(orderNumber);
                orderDto.setPostId(productNumber);
                orderDto.setStatus(OrderDto.OrderStatus.getByCode(status));
                orderDto.setOrderDate(orderDateTime);
                orderDto.setBrandName(brandName);
                orderDto.setProductName(productName);
                orderDto.setPrice(price);
                orderDto.setPaymentPrice(price);
                orderDto.setBuyerName(buyerName);
                orderDto.setRecipientName(buyerName);
                orderDto.setBuyerId(buyerId);
                orderDto.setBuyerPhoneNumber(phone);
                orderDto.setRecipientPhoneNumber(phone);
                orderDto.setBuyerMobilePhoneNumber(mobile);
                orderDto.setRecipientMobilePhoneNumber(mobile);
                orderDto.setRecipientZipCode(zipCode);
                orderDto.setRecipientAddress(address);
                //전체 정보는 memo로 내려가야 함
                orderDto.setMemo(memo);
                orderDto.setRequirements(requirement);
                //색상 사이즈 사용자 입력
                orderDto.setOptionName(optionName);
                orderDto.setDeliveryFeeType(beforeDeliveryFeeType);
                orderDto.setDeliveryType(beforeDeliveryType);
                orderDto.setDeliveryFee(beforeDeliveryFee);
                orderDto.setCourierCode(courierCode);
                orderDto.setCourierName(courierName);
                orderDto.setCourierCustomName(courierCustomName);
                orderDto.setTrackingNumber(trackingNumber);
                orderDto.setCalculateDate(calculateDateAndMessage);
                orderDto.setCalculateAmount(calculateAmount);
                orderDto.setReturnReason(returnReason);

                orderDto.setSaleCancellationReason(saleCancellationReason); // 판매취소 사유
                orderDto.setPurchaseCancellationReason(purchaseCancellationReason); // 구매취소 사유

                //주문수량의 경우 항상 1로 고정되기 때문에
                orderDto.setQuantity("1");
                //필웨이의 경우 대화채널이 주문당 무조건 1개기 때문에 해당 메세지로 구성한다.
                OrderBaseConversationDto orderBaseConversationDto = new OrderBaseConversationDto();
                orderBaseConversationDto.setOrderId(orderNumber);
                orderBaseConversationDto.setOrderUniqueId(orderNumber);
                orderBaseConversationDto.setChannelId(orderNumber);
                orderBaseConversationDto.setOrderBaseConversationMessageList(orderBaseConversationMessageDtoList);
                orderDto.getOrderBaseConversationList().add(orderBaseConversationDto);
                //결과값 주문 리스트에 해당 주문 객체를 추가한다.
                collectCallbackList.add(orderDto);
            }
        }catch(Exception e){
            log.error("Fail~~~~~",e);
        }

        return collectCallbackList;
    }
}
