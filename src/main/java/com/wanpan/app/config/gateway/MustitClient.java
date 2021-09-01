package com.wanpan.app.config.gateway;

import com.wanpan.app.config.JsoupHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MustitClient {
    private final String BASE_URL = "https://mustit.co.kr";
    private final String LOGIN_URL = "/member/login";
    private final String SALE_REGISTRATION_URL = "/product/add02";
    private final String SALE_MODIFICATION_URL = "/product/add02_modify/${saleId}";
    private final String SALE_DELETION_URL = "/product/product_delete";
    private final String SALE_SOLD_OUT_PAGE_URL = "/mypage/my_soldout_seller?searchType=number&searchKeyword=${saleId}";
    private final String SALE_SELLING_PAGE_URL = "/mypage/my_sell_control_seller?searchType=number&searchKeyword=${saleId}";
    private final String UPDATE_TO_SALE_STOP_REQUEST_URL = "/mypage/stop_selling/stop/${saleId}";
    private final String UPDATE_TO_SALE_STOP_CANCEL_REQUEST_URL = "/mypage/stop_selling/release/${saleId}";
    private final String PRODUCT_IMAGE_UPLOAD_URL = "/product/upload_file_ignore";
    private final String PRODUCT_MAIN_IMAGE_CHANGE_REQUEST_URL = "/layer/img_change/${saleId}";
    private final String MYPAGE_URL = "/mypage/mypage_home_seller";
    private final String ALL_ONGOING_ORDERS_FETCH_URL = "/mypage/sell_product_ignore";
    private final String DELIVERY_REQUEST_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=3";
    private final String DELIVERY_READY_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=3&order_notify=1";
    private final String DELIVERING_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=4";
    private final String DELIVERY_COMPLETION_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=4&delivery_complete=1";
    private final String BUY_CANCEL_REQUEST_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=7";
    private final String EXCHANGE_RETURN_REQUEST_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=8";
    private final String RETURN_COMPLETION_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=9";
    private final String SALE_CANCEL_ORDERS_FETCH_URL = ALL_ONGOING_ORDERS_FETCH_URL + "?stats=11";
    private final String ALL_COMPLETED_ORDERS_FETCH_URL = "/mypage/sell_complete_ignore?searchStartDate=${searchStartDate}&searchEndDate=${searchEndDate}";
    private final String CALCULATION_SCHEDULE_ORDERS_FETCH_URL = ALL_COMPLETED_ORDERS_FETCH_URL + "&stats=5";
    private final String CALCULATION_COMPLETION_ORDERS_FETCH_URL = ALL_COMPLETED_ORDERS_FETCH_URL + "&stats=6";
    private final String BUY_CANCEL_COMPLETION_ORDERS_FETCH_URL = ALL_COMPLETED_ORDERS_FETCH_URL + "&stats=14";
    private final String RETURN_REFUND_COMPLETION_ORDERS_FETCH_URL = ALL_COMPLETED_ORDERS_FETCH_URL + "&stats=10";
    private final String SALE_CANCEL_COMPLETION_ORDERS_FETCH_URL = ALL_COMPLETED_ORDERS_FETCH_URL + "&stats=12";
    private final String ORDER_CONVERSATION_URL = "/popup/all_talk_no/${orderUniqueId}";
    private final String ONGOING_ORDERS_PAGE_URL = "/mypage/my_sell_product_seller?&per_page=${offset}";
    private final String COMPLETE_ORDERS_PAGE_URL = "/mypage/my_sell_complete_seller?searchStartDate=${searchStartDate}&searchEndDate=${searchEndDate}&per_page=${offset}";
    private final String CALCULATION_REQUEST_ORDERS_PAGE_URL = COMPLETE_ORDERS_PAGE_URL + "&stats=5";
    private final String ORDER_DETAIL_PAGE_URL = "/mypage/order_detail_seller/${orderUniqueId}";
    private final String ORDER_DELIVERY_INFO_PAGE_URL = "/layer/info_delivery_ignore/${orderUniqueId}?flag=Y";
    private final String ORDER_EXCHANGE_DELIVERY_INFO_PAGE_URL = ORDER_DELIVERY_INFO_PAGE_URL + "&exchange_yn=Y";
    private final String UPDATE_TO_DELIVERY_READY_URL = "/layer/product_order/${orderUniqueId}";
    private final String UPDATE_TO_DELIVERY_URL = "/layer/product_send/${orderUniqueId}";
    private final String UPDATE_TO_PAYMENT_COMPLETE_BY_EXCHANGE_BY_EXCHANGE_CONFIRM_URL = "/layer/recall_ok/${orderUniqueId}";
    private final String UPDATE_TO_DELIVERY_BY_REJECT_URL = "/layer/order_reject_ok/${orderUniqueId}";
    private final String UPDATE_TO_RETURN_COMPLETE_BY_RETURN_CONFIRM_URL = "/layer/return_ok/${orderUniqueId}";
    private final String UPDATE_TO_SELL_CANCEL_COMPLETE_URL = "/layer/order_cancel/${orderUniqueId}";
    private final String UPDATE_TO_BUY_CANCEL_COMPLETE_URL = "/layer/order_cancel_ok/${orderUniqueId}";
    private final String UPDATE_CALCULATION_DELAY_FLAG_URL = "/front_ajax/calcuSubmit";

    private final String SALE_REGISTRATION_REFERER_URL = "/product/add02";

    //https://mustit.co.kr/front_ajax/getBrand?ajax_type=brand2&str=BEST
    //get이 붙은 메소드의 경우 Json형식으로 내려주지만 데이타가 부족해서 BEST경우만 사용되는걸로 보임
    private final String BRAND_BEST_AJAX_URL = "/front_ajax/getBrand";
    //https://mustit.co.kr/front_ajax/brand2?ajax_type=add02&str=GUI
    //span tag 형태의 html 스트링으로 내려와서 전부 파싱해야 한다.
    private final String BRAND_SEARCH_AJAX_URL = "/front_ajax/brand2";
    //https://mustit.co.kr/front_ajax/getHeadCate
    private final String CATEGORY_HEAD_AJAX_URL = "/front_ajax/getHeadCate";
    //https://mustit.co.kr/front_ajax/getSubCateHtml
//    private final String CATEGORY_SUB_AJAX_URL = "/front_ajax/getSubCateHtml";
    private final String CATEGORY_SUB_AJAX_URL = "/front_ajax/getSubCateHtml";

    //https://mustit.co.kr/front_ajax/getFilterHtml
    private final String CATEGORY_FILTER_AJAX_URL = "/front_ajax/getFilterHtml";

    //https://mustit.co.kr/mypage/order_question_seller?searchAnswer=N&search_date=12m
    private final String QNA_URL = "/mypage/order_question_seller";
    private final String QNA_ANSWER_POST_URL = "/mypage/order_me_question_insert";

    private final String NOTICE_LIST_GET_URL = "/board/notice_seller";
    private final String NOTICE_DETAIL_GET_URL = "/board/notice_view_seller";


    /**
     * 로그인에 대한 결과를 가져온다.
     * 이때 받아온 Header에 SetCookie값을 이용하는데 여기서 꼭 들어가야 하는 값은 PHPSESSID, AWSALB, AWSALBCORS 세가지 이다.
     */
    public Connection.Response login(Map<String, String> data, String requestBody) throws IOException {

        return JsoupHttpClient.post(BASE_URL + LOGIN_URL,null, data, requestBody, null, false, false, false);
    }

    //https://mustit.co.kr/front_ajax/brand2?ajax_type=add02&str=GUI
    public Connection.Response getBrands(Map<String, String> headers, Map<String, String> data) throws IOException {

        return JsoupHttpClient.get(BASE_URL + BRAND_SEARCH_AJAX_URL, headers, data, null, false, false, false);
    }

    /**
     * 메인 Category 목록을 가져온다
     */
    public Connection.Response getHeadCategory(Map<String, String> headers, Map<String, String> data, String requestBody) throws IOException {

        return JsoupHttpClient.post(BASE_URL + CATEGORY_HEAD_AJAX_URL,headers, data, requestBody, null, false, false, false);
    }

    /**
     * Sub Category 목록을 가져온다
     */
    public Connection.Response getSubCategory(Map<String, String> headers, Map<String, String> data, String requestBody) throws IOException {
        log.info("headers: {}, data:{}",headers, data);
        return JsoupHttpClient.post(BASE_URL + CATEGORY_SUB_AJAX_URL,headers, data, requestBody, null, false, false, false);
    }

    /**
     * Sub Category 목록을 가져온다
     */
    public Connection.Response getFilterHtml(Map<String, String> headers, Map<String, String> data, String requestBody) throws IOException {
        log.info("headers: {}, data:{}",headers, data);
        return JsoupHttpClient.post(BASE_URL + CATEGORY_FILTER_AJAX_URL,headers, data, requestBody, null, false, false, false);
    }

    /**
     * 마이 페이지를 가져온다.
     */
    public Connection.Response getMyPage(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + MYPAGE_URL, headers, null, null, false, false, false);
    }

    /**
     * 판매 수정 페이지를 가져온다.
     */
    public Connection.Response getSaleModificationPage(Map<String, String> headers, String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + SALE_MODIFICATION_URL.replace("${saleId}", saleId), headers, null, null, false, false, false);
    }

    /**
     * 1. 진행주문관리 > 발송요청 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getDeliveryRequestOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + DELIVERY_REQUEST_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 2. 진행주문관리 > 발송준비중 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getDeliveryReadyOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + DELIVERY_READY_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 3. 진행주문관리 > 배송중 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getDeliveringOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + DELIVERING_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 4. 진행주문관리 > 배송완료 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getDeliveryCompletionOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + DELIVERY_COMPLETION_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 5. 진행주문관리 > 구매취소요청 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getBuyCancelRequestOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + BUY_CANCEL_REQUEST_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 6. 진행주문관리 > 교환반품요청 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getExchangeReturnRequestOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + EXCHANGE_RETURN_REQUEST_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 7. 진행주문관리 > 반품성사 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getReturnCompletionOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + RETURN_COMPLETION_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 8. 진행주문관리 > 판매취소 상태인 주문 목록을 가져온다.
     */
    public Connection.Response getSaleCancelOrderList(Map<String, String> headers) throws IOException {

        return JsoupHttpClient.get(BASE_URL + SALE_CANCEL_ORDERS_FETCH_URL, headers, null, null, true, false, false);
    }

    /**
     * 9. 완료주문관리 > 정산예정 상태인 주문 목록을 결제기간 범위로 가져온다.
     */
    public Connection.Response getCalculationScheduleOrderList(Map<String, String> headers, String searchStartDate, String searchEndDate) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + CALCULATION_SCHEDULE_ORDERS_FETCH_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                , headers, null, null, true, false, false);
    }

    /**
     * 10. 완료주문관리 > 정산완료 상태인 주문 목록을 결제기간 범위로 가져온다.
     */
    public Connection.Response getCalculationCompletionOrderList(Map<String, String> headers, String searchStartDate, String searchEndDate) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + CALCULATION_COMPLETION_ORDERS_FETCH_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate),
                headers, null, null, true, false, false);
    }

    /**
     * 11. 완료주문관리 > 구매취소완료 상태인 주문 목록을 결제기간 범위로 가져온다.
     */
    public Connection.Response getBuyCancelCompletionOrderList(Map<String, String> headers, String searchStartDate, String searchEndDate) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + BUY_CANCEL_COMPLETION_ORDERS_FETCH_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                , headers, null, null, true, false, false);
    }

    /**
     * 12. 완료주문관리 > 반품환불완료 상태인 주문 목록을 결제기간 범위로 가져온다.
     */
    public Connection.Response getReturnRefundCompletionOrderList(Map<String, String> headers, String searchStartDate, String searchEndDate) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + RETURN_REFUND_COMPLETION_ORDERS_FETCH_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                , headers, null, null, true, false, false);
    }

    /**
     * 13. 완료주문관리 > 판매취소완료 상태인 주문 목록을 결제기간 범위로 가져온다.
     */
    public Connection.Response getSaleCancelCompletionOrderList(Map<String, String> headers, String searchStartDate, String searchEndDate) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + SALE_CANCEL_COMPLETION_ORDERS_FETCH_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                , headers, null, null, true, false, false);
    }

    /**
     * 주문대화를 가져온다.
     */
    public Connection.Response getOrderConversation(Map<String, String> headers, String orderUniqueId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + ORDER_CONVERSATION_URL.replace("${orderUniqueId}", orderUniqueId), headers, null, null, true, false, false);
    }

    /**
     * 진행주문관리>전체 페이지를 가져온다.
     */
    public Connection.Response getOngoingOrderListPage(Map<String, String> headers, int offset) throws IOException {

        return JsoupHttpClient.get(BASE_URL + ONGOING_ORDERS_PAGE_URL.replace("${offset}", Integer.toString(offset)), headers, null, null, false, false, false);
    }

    /**
     * 완료주문관리>전체 페이지를 가져온다.
     */
    public Connection.Response getCompleteOrderListPage(Map<String, String> headers, String searchStartDate, String searchEndDate, int offset) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + COMPLETE_ORDERS_PAGE_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                        .replace("${offset}", Integer.toString(offset))
                , headers, null, null, false, false, false);
    }


    /**
     * 완료주문관리>정산예정 페이지를 가져온다.
     */
    public Connection.Response getCalculationScheduleOrderListPage(Map<String, String> headers, String searchStartDate, String searchEndDate, int offset) throws IOException {

        return JsoupHttpClient.get(
                BASE_URL + CALCULATION_REQUEST_ORDERS_PAGE_URL
                        .replace("${searchStartDate}", searchStartDate)
                        .replace("${searchEndDate}", searchEndDate)
                        .replace("${offset}", Integer.toString(offset))
                , headers, null, null, false, false, false);
    }

    /**
     * 주문상세 페이지를 가져온다.
     */
    public Connection.Response getOrderDetailPage(Map<String, String> headers, String orderUniqueId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + ORDER_DETAIL_PAGE_URL.replace("${orderUniqueId}", orderUniqueId), headers, null, null, false, false, false);
    }

    /**
     * 배송정보 페이지를 가져온다.
     */
    public Connection.Response getOrderDeliveryInfoPage(Map<String, String> headers, String orderUniqueId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + ORDER_DELIVERY_INFO_PAGE_URL.replace("${orderUniqueId}", orderUniqueId), headers, null, null, false, false, false);
    }

    /**
     * 교환배송정보 페이지를 가져온다.
     */
    public Connection.Response getExchangeOrderDeliveryInfoPage(Map<String, String> headers, String orderUniqueId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + ORDER_EXCHANGE_DELIVERY_INFO_PAGE_URL.replace("${orderUniqueId}", orderUniqueId), headers, null, null, false, false, false);
    }


    /**
     * 판매글을 등록한다.
     */
    public Connection.Response registerSale(Map<String, String> headers, List<Pair<String, String>> data)
            throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + SALE_REGISTRATION_URL)
                .method(Connection.Method.POST)
                .headers(headers)
                .referrer(BASE_URL + SALE_REGISTRATION_REFERER_URL);

        for (Pair<String, String> pair : data) {
            connection.data(pair.getFirst(), pair.getSecond());
        }

        return connection.execute();
    }

    /**
     * 주문대화 메세지를 보낸다.
     */
    public Connection.Response sendOrderConversationMessage(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(BASE_URL + ORDER_CONVERSATION_URL.replace("${orderUniqueId}", orderUniqueId)
                , headers, data, null, null,true, false, false);
    }

    /**
     * 판매글을 수정한다.
     */
    public Connection.Response updateSale(Map<String, String> headers, List<Pair<String, String>> data, String saleId)
            throws IOException {
        String saleUpdateUrl = BASE_URL + SALE_MODIFICATION_URL.replace("${saleId}", saleId);
        Connection connection = Jsoup.connect(saleUpdateUrl)
                .method(Connection.Method.POST)
                .headers(headers)
                .referrer(saleUpdateUrl);

        for (Pair<String, String> pair : data) {
            connection.data(pair.getFirst(), pair.getSecond());
        }

        return connection.execute();
    }

    /**
     * 판매등록 관련 상품 이미지를 업로드한다.
     */
    public Connection.Response uploadProductImage(String fileNameWithExtension, InputStream inputStream) throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_IMAGE_UPLOAD_URL)
                .method(Connection.Method.POST);
        connection.data("name", fileNameWithExtension);
        connection.data("file", fileNameWithExtension, inputStream);

        return connection.execute();
    }

    /**
     * 판매수정 관련 상품 대표 이미지에 대해 변경 요청한다.
     */
    public Connection.Response updateProductMainImage(String fileNameWithExtension, InputStream inputStream, String saleId) throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_MAIN_IMAGE_CHANGE_REQUEST_URL.replace("${saleId}", saleId))
                .method(Connection.Method.POST);
        connection.data("product_number", saleId);
        connection.data("userfile", fileNameWithExtension, inputStream);

        return connection.execute();
    }

    /**
     * "발송요청"에서 "배송준비중"으로 주문상태 업데이트
     */
    public Connection.Response updateOrderStatusToDeliveryReady(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_DELIVERY_READY_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }


    /**
     * (1) "발송요청"에서 "배송중"으로 업데이트
     * (2) 교환승인["교환요청"에서 "배송중(교환)"으로 업데이트]
     * (3) 교환승인["발송요청(교환)"에서 "배송중(교환)"으로 업데이트]
     * (4) 교환승인["배송준비중(교환)"에서 "배송중(교환)"으로 업데이트]
     */
    public Connection.Response updateOrderStatusToDelivery(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_DELIVERY_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * 교환승인["교환요청"에서 "발송요청(교환)"으로 업데이트]
     */
    public Connection.Response updateOrderStatusToPaymentCompleteByExchange(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_PAYMENT_COMPLETE_BY_EXCHANGE_BY_EXCHANGE_CONFIRM_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * 반품승인["반품요청"에서 "반품환불완료" 또는 "반품성사"로 업데이트
     */
    public Connection.Response updateOrderStatusToReturnComplete(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_RETURN_COMPLETE_BY_RETURN_CONFIRM_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * (1) "교환요청"에서 교환거절하는 경우
     * (2) "반품요청"에서 반품거절하는 경우
     * (3) "구매취소요청"에서 구매취소거절하는 경우
     */
    public Connection.Response updateOrderStatusToDeliveryByReject(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_DELIVERY_BY_REJECT_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * (1) "발송요청" 에서..
     * (2) "발송요청(교환)"
     * (3) "배송준비중" 에서..
     * (4) "배송준비중(교환)" 에서..
     *
     * 판매취소하는 경우
     */
    public Connection.Response updateOrderStatusToSellCancelComplete(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_SELL_CANCEL_COMPLETE_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * "구매취소요청"에서 구매취소승인하는 경우
     */
    public Connection.Response updateOrderStatusToBuyCancelComplete(Map<String, String> headers, Map<String, String> data, String orderUniqueId) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_TO_BUY_CANCEL_COMPLETE_URL.replace("${orderUniqueId}", orderUniqueId),
                headers, data, null, null, false, false, false);
    }

    /**
     * (1) "정산예정"에서 "정산보류중"으로 업데이트
     * (2) "정산보류중"에서 "정산예정"으로 업데이트
     */
    public Connection.Response updateCalculationDelayFlag(Map<String, String> headers, Map<String, String> data) throws IOException {

        return JsoupHttpClient.post(
                BASE_URL + UPDATE_CALCULATION_DELAY_FLAG_URL,
                headers, data, null, null, false, false, false);
    }

    /**
     * 판매글을 삭제한다.
     */
    public Connection.Response deleteSale(String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + SALE_DELETION_URL + "/" + saleId, null, null, null, false, false, false);
    }

    /*
     * 상품문의 페이지에 대해서 타입에 따라 대기와 완료를 가져온다.
     */
    public Connection.Response getQna(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(BASE_URL + QNA_URL, headers, data, cookies,  false, false, true);
    }

    /**
     *
     * @param headers 요청헤더
     * @param data 요청 data
     * @param cookies 로그인 쿠키값들
     * @return Response
     * @throws IOException IOException
     */
    public Connection.Response postAnswerForQna(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(BASE_URL + QNA_ANSWER_POST_URL,headers, data, null, cookies, false, false, false);
    }

    /*
     * 상품문의 페이지에 대해서 타입에 따라 대기와 완료를 가져온다.
     */
    public Connection.Response getShopNotice() throws IOException {

        return JsoupHttpClient.get(BASE_URL + NOTICE_LIST_GET_URL, null, null, null,  false, false, true);
    }

    /*
     * 상품문의 페이지에 대해서 타입에 따라 대기와 완료를 가져온다.
     */
    public Connection.Response getShopNoticeDetail(Map<String, String> data) throws IOException {

        return JsoupHttpClient.get(BASE_URL + NOTICE_DETAIL_GET_URL, null, data, null,  false, false, true);
    }

    /**
     * 해당 판매글번호에 대한 품절상품관리 페이지를 가져온다
     */
    public Connection.Response getSaleSoldOutPage(Map<String, String> headers, String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + SALE_SOLD_OUT_PAGE_URL.replace("${saleId}", saleId), headers, null, null, false, false, false);
    }

    /**
     * 해당 판매글번호에 대한 판매상품관리 페이지를 가져온다
     */
    public Connection.Response getSaleSellingPage(Map<String, String> headers, String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + SALE_SELLING_PAGE_URL.replace("${saleId}", saleId), headers, null, null, false, false, false);
    }

    /**
     * 판매글의 상태를 판매중지로 변경한다
     */
    public Connection.Response updateSaleStatusToSaleStop(Map<String, String> headers, String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + UPDATE_TO_SALE_STOP_REQUEST_URL.replace("${saleId}", saleId), headers, null, null, false, false, false);
    }

    /**
     * 판매글의 판매중지 상태를 해제한다
     */
    public Connection.Response updateSaleStatusToSaleStopCancel(Map<String, String> headers, String saleId) throws IOException {

        return JsoupHttpClient.get(BASE_URL + UPDATE_TO_SALE_STOP_CANCEL_REQUEST_URL.replace("${saleId}", saleId), headers, null, null, false, false, false);
    }

    public enum HeadCategoryType {
        WOMEN("W"),
        MEN("M"),
        KIDS("K"),
        LIFE("L"),
        SHARE("W,M");

        private final String code;

        HeadCategoryType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

}
