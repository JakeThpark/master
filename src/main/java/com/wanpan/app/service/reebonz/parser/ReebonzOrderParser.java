package com.wanpan.app.service.reebonz.parser;

import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.reebonz.ReebonzProductOptionStock;
import com.wanpan.app.dto.reebonz.ReebonzWebPageProductUpdate;
import com.wanpan.app.service.reebonz.ReebonzCourier;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReebonzOrderParser {
    private static final String HTML_TAG_REGEX = "<[\\/a-z-\\s=\"]+>";

    public static List<OrderDto.Request.CollectCallback> parseOrderListFromWebPage(final String htmlContents, boolean orderCompleteFlag) {
        List<OrderDto.Request.CollectCallback> collectCallbackList = new ArrayList<>();

        Document document = Jsoup.parse(htmlContents);
        String orderElementClassName = orderCompleteFlag ? "table-layout order-list processed-items order-done" : "table-layout order-list";
        Elements orderElements = document.getElementsByClass(orderElementClassName);
        if(orderElements.size() == 0){
            log.info("Not exist Order Table");
            return collectCallbackList;
        }else if(orderElements.size() > 1){
            log.info("Order Table over size:{}", orderElements.size());
            return collectCallbackList;
        }

        Elements orderBlockElements = orderElements.get(0).select("li.table-row");
        for(Element orderBlockElement : orderBlockElements){
            log.info("=====================================");
//            log.info("{}", orderBlockElement);
            //주문번호, 날짜 칼럼
            Element orderNumberPanelElement = orderBlockElement.select("div.table-cell.col1.order-number-panel").first();
            Element orderNumberElement = orderNumberPanelElement.select("a.order-number").first();
            String[] orderDetailLink = orderNumberElement.attr("href").split("/");
            String orderUniqueNumber = orderDetailLink[orderDetailLink.length-1];
            String orderNumber = orderNumberElement.html().replaceAll(HTML_TAG_REGEX,"");
            String orderDate = orderNumberPanelElement.getElementsByTag("span").html();

            //주문정보 칼럼
            Element infoBlockElement = orderBlockElement.select("div.table-cell.ordered-item-info-panel").first();

            //상품/구매자 정보 칼럼
            Element orderedItemInfo = infoBlockElement.select("div.table-cell.col2.ordered-item-info").first();
            Element itemInfo = orderedItemInfo.select("div.product-info > div.item-info").first();
            String brandName = itemInfo.select("p.brand-name").first().html();
            String productNameElementClassName = orderCompleteFlag ? "p.product-name" : "a.product-name";
            Element productNameNumber = itemInfo.select(productNameElementClassName).first();
            String productName = PatternExtractor.REEBONZ_PRODUCT_NAME.extractDefaultValue(productNameNumber.html(),1, productNameNumber.html());
            String[] productLink = productNameNumber.attr("href").split("/");
            String productNumber = productLink[productLink.length-1];
            String reebonzSku = itemInfo.select("dl.meta-info > dd").first().html();

            //옵션,주문수량
            String optionInfo = orderedItemInfo.select("div > dl > dd.option-info").first().html();
            Map<Integer,String> optionMap = PatternExtractor.REEBONZ_OPTION_AMOUNT_INFO.extractGroups(optionInfo);
            String memo = optionMap.get(1);
            String classificationValue = optionMap.get(2);
            String optionName = optionMap.get(3);
            String amount = optionMap.get(4);

            if(memo == null){
                memo = PatternExtractor.REEBONZ_OPTION_NAME_AMOUNT.extractDefaultValue(optionInfo, 1, null);
            }

            //구매인,연락처
            Elements buyInfoElements = orderedItemInfo.select("div > div.order-info > dl > dd");
            String buyerInfo = buyInfoElements.first().html();
            log.info("buyerInfo:{}",buyerInfo);
            Map<Integer,String> buyerMap = PatternExtractor.REEBONZ_BUYER_NAME_PHONE.extractGroups(buyerInfo);
            String buyerName = buyerMap.get(1);
            String buyerPhone = buyerMap.get(2);

            //주문 메세지가 존재하는 경우
            String buyerMessage = null;
            if(buyInfoElements.size() > 1){
                buyerMessage = buyInfoElements.last().html();
            }

            //판매가/정산가
            Element productPriceInfo = infoBlockElement.select("div.table-cell.col3.product-price").first();
            String sellPrice = productPriceInfo.select("p").first().html().replace(",","").replace("원","");
            Element calculationPriceElement = productPriceInfo.select("p.payment-price").first();
            String calculationPrice = null;
            if (calculationPriceElement != null) {
                calculationPrice = calculationPriceElement.html().replace(",","").replace("원","");
            }

            //진행상태
            Element statusTrackingElement = infoBlockElement.select("div.table-cell.col4.ordered-item-deliv-panel").first();
            String status = statusTrackingElement.select("dl > dd.delivery-status").first().html();
            Element trackingTitleElement = statusTrackingElement.select("dl > dt.tracking-title").first();
            String trackingTitle = null;
            if (trackingTitleElement != null) {
                trackingTitle = trackingTitleElement.html();
            }
//            log.info("statusTrackingElement: {}",statusTrackingElement);
            //송장,택배사(있을 경우 저장한다.)
            String trackingNumber = null;
            String courierName = null;
            String courierCode = null;
            Element trackingInfoElement = statusTrackingElement.select("dl > dd.tracking-number > a.first-delivery").first();
            if (!ObjectUtils.isEmpty(trackingInfoElement)) {
                Map<Integer, String> trackingInfoMap = PatternExtractor.REEBONZ_TRACKING_INFO.extractGroups(trackingInfoElement.html());
                trackingNumber = trackingInfoMap.get(1).trim();
                courierName = trackingInfoMap.get(2);
//                log.info("courierName:{}",courierName);
                //TODO:반품요청 시에 셀렉박스 없는 경우 존재
                courierCode = ReebonzCourier.fromName(courierName).getCode();
//                log.info("courierCode:{}",courierCode);
            }

            //기타 버튼들
            Element statusButtonElement = infoBlockElement.select("div.table-cell.col5.ordered-item-control-panel").first();
            boolean existOrderConfirmBtn = false;
            boolean existSellCancelBtn = false;
            boolean existCancelConfirmBtn = false;
            boolean existReturnConfirmBtn = false;
            boolean existReturnCompleteBtn = false;
            boolean existReturnRejectBtn = false;
            boolean existReebonzQuestionBtn = false;
            boolean existMemoBtn = false;
            if (statusButtonElement != null) {
                //주문확인 버튼
                if(statusButtonElement.select("input.control-btn.confirm-btn").size() > 0){
                    existOrderConfirmBtn = true;
                }
                //품절 버튼
                if(statusButtonElement.select("input.control-btn.soldout-btn").size() > 0){
                    existSellCancelBtn = true;
                }
                //취소 확인 버튼
                if(statusButtonElement.select("input.control-btn.full-size.cancel-confirm").size() > 0){
                    existCancelConfirmBtn = true;
                }
                //반품 확인 버튼
                if(statusButtonElement.select("input.control-btn.pop-modal").select("[data-target-id=refundMsg]").select("[data-type=refund_confirm]").size() > 0){
                    existReturnConfirmBtn = true;
                }
                //반품 완료 버튼
                if(statusButtonElement.select("input.control-btn.pop-modal").select("[data-target-id=refundMsg]").select("[data-type=refund_true]").size() > 0){
                    existReturnCompleteBtn = true;
                }
                //반품 거절 버튼
                if(statusButtonElement.select("input.control-btn.pop-modal").select("[data-target-id=refundMsg]").select("[data-type=refund_refuse]").size() > 0){
                    existReturnRejectBtn = true;
                }
                //리본즈 문의 버튼
                if(statusButtonElement.select("input.control-btn.pop-modal.writing-memo-btn").select("[data-target-id=writeQna]").size() > 0){
                    existReebonzQuestionBtn = true;
                }
                //메모 작성 버튼
                if(statusButtonElement.select("input.control-btn.pop-modal.writing-memo-btn").select("[data-target-id=writeMemo]").size() > 0){
                    existMemoBtn = true;
                }
            }

            //메모
//            log.info("주문메모 시작==================");
            Elements memoElements = infoBlockElement.select("div.table-row.ordered-item-memo-panel.has-comment > div.table-cell > ul > li");
            for(Element memoElement : memoElements){
                String memoDate = memoElement.select("span.memo-icon").first().html().replaceAll(HTML_TAG_REGEX,"");
                String memoText = memoElement.select("p").first().html();
//                log.info("Date:{}, Text:{}",memoDate, memoText);
            }
//            log.info("주문메모 끝==================");

            OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
//            log.info("orderNumber:{}",orderNumber);
            orderDto.setOrderId(orderNumber);

//            log.info("orderUniqueNumber:{}",orderUniqueNumber);
            orderDto.setOrderUniqueId(orderUniqueNumber);

//            log.info("orderDate:{}",orderDate);
            orderDto.setOrderDate(convertStrToLocalDateTime(orderDate));

//            log.info("brandName:{}",brandName);
            orderDto.setBrandName(brandName);

//            log.info("productName:{}",productName);
            orderDto.setProductName(productName);

//            log.info("productNumber:{}",productNumber); //현재 사용안함

//            log.info("reebonzSku:{}",reebonzSku);
            orderDto.setCustomizedSku(reebonzSku);

//            log.info("memo:{}",memo);
            orderDto.setMemo(memo);

//            log.info("optionName:{}",optionName);
            orderDto.setOptionName(optionName);

//            log.info("classificationValue:{}",classificationValue);
            orderDto.setClassificationValue(classificationValue);

//            log.info("amount:{}",amount);
            orderDto.setQuantity(amount);

//            log.info("구매인:{}",buyerName);
            orderDto.setBuyerName(buyerName);

//            log.info("구매인 연락처:{}",buyerPhone);
            orderDto.setBuyerPhoneNumber(buyerPhone);
            orderDto.setRecipientPhoneNumber(buyerPhone);
            orderDto.setBuyerMobilePhoneNumber(buyerPhone);
            orderDto.setRecipientMobilePhoneNumber(buyerPhone);

//            log.info("주문메세지:{}",buyerMessage);
//            log.info("sellPrice:{}",sellPrice);
//            log.info("supplyPrice:{}",supplyPrice);
//
//            log.info("tracking:{}",tracking);
//            log.info("trckingNumber:{}",trckingNumber);
            orderDto.setTrackingNumber(trackingNumber);

//            log.info("courierCode:{}",courierCode);
            orderDto.setCourierCode(courierCode);
            orderDto.setReturnCourierCode(courierCode);
            orderDto.setExchangeCourierCode(courierCode);

//            log.info("courierName:{}",courierName);
            orderDto.setCourierName(courierName);
            orderDto.setReturnCourierName(courierName);
            orderDto.setExchangeCourierName(courierName);

            //버튼별 상태값 산정 체크
//            log.info("status:{}",status);
//            log.info("주문확인 버튼: {}",existOrderConfirmBtn);
//            log.info("품절 버튼: {}",existSellCancelBtn);
//            log.info("취소확인 버튼: {}",existCancelConfirmBtn);
//            log.info("반품확인 버튼: {}",existReturnConfirmBtn);
//            log.info("반품완료 버튼: {}",existReturnCompleteBtn);
//            log.info("반품거절 버튼: {}",existReturnRejectBtn);
//            log.info("리본즈문의 버튼: {}",existReebonzQuestionBtn);
//            log.info("메모작성 버튼: {}",existMemoBtn);
            orderDto.setStatus(
                    getOrderStatusByStatusAndBtns(
                            status,
                            existOrderConfirmBtn,
                            existSellCancelBtn,
                            existCancelConfirmBtn,
                            existReturnConfirmBtn,
                            existReturnCompleteBtn,
                            existReturnRejectBtn
                    )
            );
            log.info("주문상태: {}",orderDto.getStatus());
            collectCallbackList.add(orderDto);
        }

        return collectCallbackList;
    }

    public static List<OrderDto.Request.CollectCallback> parseOrderListFromExcelHtml(final String htmlContents) {
        log.info("Call parseOrderListFromExcelHtml!");
        List<OrderDto.Request.CollectCallback> collectCallbackList = new ArrayList<>();

        Document document = Jsoup.parse(htmlContents);
        Elements orderElements = document.select("body > table > tbody > tr");
        for(Element orderElement : orderElements){ //주문별 블럭
            Elements orderInfoTdElements = orderElement.getElementsByTag("td"); //엑셀칼럼 대칭(총 23개 정보)
//            log.info("=======================Excel Log=======================");
//            log.info("주문번호:{}",orderInfoTdElements.get(0).html());
//            log.info("주문일:{}",orderInfoTdElements.get(1).html());
//            log.info("상품상태:{}",orderInfoTdElements.get(2).html());
//            log.info("공급가:{}",orderInfoTdElements.get(3).html());
//            log.info("판매가:{}",orderInfoTdElements.get(4).html());
//            log.info("쿠폰가:{}",orderInfoTdElements.get(5).html());
//            log.info("수량:{}",orderInfoTdElements.get(6).html());
//            log.info("쿠폰할인금액:{}",orderInfoTdElements.get(7).html());
//            log.info("SKU:{}",orderInfoTdElements.get(8).html());
//            log.info("상품DBID:{}",orderInfoTdElements.get(9).html());
//            log.info("상품상세:{}",orderInfoTdElements.get(10).html());
//            log.info("상품상세2:{}",PatternExtractor.REEBONZ_EXCEL_PRODUCT_NAME.extractDefaultValue(orderInfoTdElements.get(10).html(),1,orderInfoTdElements.get(10).html()));
//
//            log.info("주문수량:{}",orderInfoTdElements.get(11).html());
//            log.info("구매인:{}",orderInfoTdElements.get(12).html());
//            log.info("수령인:{}",orderInfoTdElements.get(13).html());
//            log.info("수령인 연락처:{}",orderInfoTdElements.get(14).html());
//            log.info("배송지:{}",orderInfoTdElements.get(15).html());
//            log.info("배송상태:{}",orderInfoTdElements.get(16).html());
//            log.info("주문 메세지:{}",orderInfoTdElements.get(17).html());
//            log.info("배송 메세지:{}",orderInfoTdElements.get(18).html());
//            log.info("개인통관고유부호:{}",orderInfoTdElements.get(19).html());
//            log.info("결제수단:{}",orderInfoTdElements.get(20).html());
//            log.info("경유 배송송장:{}",orderInfoTdElements.get(21).html());
//            log.info("판매자SKU:{}",orderInfoTdElements.get(22).html());
//            log.info("==================================================");

            OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
            //주문번호
            Map<Integer,String> orderNumberMap = PatternExtractor.REEBONZ_EXCEL_ORDER_NUMBER.extractGroups(orderInfoTdElements.get(0).html());
            orderDto.setOrderId(orderNumberMap.get(1));
            orderDto.setOrderUniqueId(orderNumberMap.get(2));
            //주문일
            orderDto.setOrderDate(convertExcelStrToLocalDateTime(orderInfoTdElements.get(1).html().trim()));
            //상품상태
            orderDto.setStatus(OrderDto.OrderStatus.getByCode(orderInfoTdElements.get(2).html().trim()));
            //공급가
            orderDto.setPaymentPrice(Long.parseLong(orderInfoTdElements.get(3).html().replace(",","")));
            //판매가
            orderDto.setPrice(Long.parseLong(orderInfoTdElements.get(4).html().replace(",","")));
            //쿠폰가(쿠폰할인된금액)
            orderDto.setCouponDiscountPrice(Long.parseLong(orderInfoTdElements.get(5).html().replace(",","")));
            //수량(주문수량과 현재 동일 값으로 보임)
            orderInfoTdElements.get(6).html();
            //쿠폰할인금액(쿠폰금액)
            orderDto.setCouponPrice(Long.parseLong(orderInfoTdElements.get(7).html().replace(",","")));
            //SKU
            orderDto.setCustomizedSku(orderInfoTdElements.get(8).html());
            //상품DBID
            orderDto.setPostId(orderInfoTdElements.get(9).html());
            //상품상세(상품명,옵션)
            orderDto.setProductName(
                    PatternExtractor.REEBONZ_EXCEL_PRODUCT_NAME.extractDefaultValue(orderInfoTdElements.get(10).html(),1,orderInfoTdElements.get(10).html())
            );
            //주문수량
            orderDto.setQuantity(orderInfoTdElements.get(11).html());
            //구매인 정보(이름 (일반) (모바일번호))
            Map<Integer,String> buyerInfoMap = PatternExtractor.REEBONZ_EXCEL_BUYER_INFO.extractGroups(orderInfoTdElements.get(12).html());
            orderDto.setBuyerName(buyerInfoMap.get(1));
            orderDto.setBuyerPhoneNumber(buyerInfoMap.get(2));
            orderDto.setBuyerMobilePhoneNumber(buyerInfoMap.get(2));
            //수령인
            orderDto.setRecipientName(orderInfoTdElements.get(13).html());
            //수령인연락처
            orderDto.setRecipientPhoneNumber(orderInfoTdElements.get(14).html());
            orderDto.setRecipientMobilePhoneNumber(orderInfoTdElements.get(14).html());
            //배송지([우편번호] 주소)
            Map<Integer,String> addressInfoMap = PatternExtractor.REEBONZ_EXCEL_ADDRESS_INFO.extractGroups(orderInfoTdElements.get(15).html());
            orderDto.setRecipientZipCode(addressInfoMap.get(1));
            orderDto.setRecipientAddress(addressInfoMap.get(2));
            //배송상태
            orderInfoTdElements.get(16).html();
            //주문메세지
            orderDto.setRequirements(orderInfoTdElements.get(17).html());
            //배송메세지
            orderDto.setDeliveryMessage(orderInfoTdElements.get(18).html());
            //개인통관고유부호
//            orderInfoTdElements.get(19).html();
            //결제수단
//            orderInfoTdElements.get(20).html();
            //경유배송송장
//            orderInfoTdElements.get(21).html();
            //판매자SKU - 현재는 리본즈에서 사용안함
//            orderInfoTdElements.get(22).html();
            collectCallbackList.add(orderDto);
        }

        return collectCallbackList;
    }

    public static OrderDto.OrderStatus getOrderStatusByStatusAndBtns(
            String status,
            boolean existOrderConfirmBtn, //주문확인 버튼
            boolean existSellCancelBtn, //품절 버튼
            boolean existCancelConfirmBtn, //취소확인 버튼
            boolean existReturnConfirmBtn, //반품확인 버튼
            boolean existReturnCompleteBtn, //반품완료 버튼
            boolean existReturnRejectBtn //반품거절 버튼
    ){
        OrderDto.OrderStatus orderStatus = OrderDto.OrderStatus.getByCode(status);
        if(ObjectUtils.isEmpty(orderStatus)){
            return null;
        }
        switch(orderStatus){
            case PAYMENT_COMPLETE: //주문완료 상태(품절버튼 존재에 따라서 품절요청인지 아닌지 판단)
                if(!existSellCancelBtn){ //품절버튼 없으면 판매취소상태
                    return OrderDto.OrderStatus.SELL_CANCEL;
                }else{
                    return OrderDto.OrderStatus.PAYMENT_COMPLETE;
                }
            case RETURN_REQUEST:
                if(existReturnConfirmBtn){ //반품요청상태, 반품확인버튼만 존재(송장정보존재) - 반품을 최초 요청한 상태
                    return OrderDto.OrderStatus.RETURN_REQUEST;
                }else if(existReturnCompleteBtn && existReturnRejectBtn){ //반품요청상태, 반품완료,반품거절버튼 존재 - 반품을 확인한 상태(RETURN_CONFIRM)
                    return OrderDto.OrderStatus.RETURN_CONFIRM;
                }else if(!existReturnCompleteBtn && !existReturnRejectBtn){
                    //반품요청상태, 반품확인,반품완료,반품거절 버튼 없을 경우 - 반품을 완료승인한 상태
                    //반품요청상태, 반품확인,반품완료,반품거절 버튼 없을 경우, 송장정보가 존재 - 반품을 구매자가 철회했다 다시 요청할때 - 실질적으로 반품완료상태와 동일(판매자 액션이 없음)
                    return OrderDto.OrderStatus.RETURN_COMPLETE;
                }else{
                    //TODO:상태가 오류난 경우
                    log.error("Failed get OrderStatus");
                    return null;
                }
            default:
                //배송준비중, 배송중
                return orderStatus;
        }
    }

    /**
     * 기존 상품 판매글 내용 중 초기화 대상 내용을 구한다
     */
    public static ReebonzWebPageProductUpdate.ProductInfoUpdateTarget parseProductInfoClearFromWebPage(final String htmlContents) {
        ReebonzWebPageProductUpdate.ProductInfoUpdateTarget productInfoUpdateTarget = new ReebonzWebPageProductUpdate.ProductInfoUpdateTarget();
        Document document = Jsoup.parse(htmlContents);

        // 상품 이미지 정보
        Elements productImageElements = document.getElementsByClass("preview-upload-file");
        for (Element uploadImageElement : productImageElements) {
            Element cancelButtonElement = uploadImageElement.getElementsByTag("button").first();
            String imageId = cancelButtonElement.attr("data-id");
            String imagetype = cancelButtonElement.attr("data-type");

            // 초기화 대상 목록에 추가
            productInfoUpdateTarget.getProductImageClearTargetList().add(
                    new ReebonzWebPageProductUpdate.ProductImageClearTarget(imageId, imagetype));
        }

        // 상품 옵션 정보
        Elements productOptionElements = document.getElementsByClass("ori-stock-item");
        for (Element productOptionElement : productOptionElements) {
            // 아이디
            Element optionIdElement = productOptionElement.getElementsByAttributeValueEnding("name","[id]").first();
            String optionId = optionIdElement.attr("value");

            // 명칭
            Element optionNameElement = productOptionElement.getElementsByAttributeValueEnding("name","[name]").first();
            String optionName = optionNameElement.attr("value");

            // 수량
            Element optionQuantityElement = productOptionElement.getElementsByAttributeValueEnding("name","[stock_count]").first();
            String optionQuantity = optionQuantityElement.attr("value");

            // 노출여부
            Element optionAvailableFlagElement = productOptionElement.getElementsByAttributeValueEnding("name","[available]").first();
            String optionAvailableFlag = optionAvailableFlagElement.attr("value");

            // 리스트에 추가
            productInfoUpdateTarget.getProductOptionUpdateTargetList().add(
                    new ReebonzProductOptionStock(optionId, optionName, optionQuantity, optionAvailableFlag));
        }

        return productInfoUpdateTarget;
    }


    //2020. 09. 16 18:11:28
    public static LocalDateTime convertStrToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    //2020. 09. 16 18:11:28
    public static LocalDateTime convertExcelStrToLocalDateTime(String dateTimeStr){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
}
