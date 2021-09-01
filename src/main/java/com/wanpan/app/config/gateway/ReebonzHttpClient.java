package com.wanpan.app.config.gateway;

import com.wanpan.app.config.JsoupHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReebonzHttpClient {

    private final String BASE_URL = "https://www.reebonz.co.kr";
    private final String BASE_PARTNER_URL = "https://partner.reebonz.co.kr";
    private final String DEV_BASE_URL = "http://dev.reebonz.co.kr:3007";
    //Login====================
    private final String LOGIN_POST_URL = "/users/sign_in";
    private final String DASHBOARD_GET_URL = "/partner/dashboard";
    // Sale
    private final String PRODUCT_SALE_POST_URL = "/partner/product/market_products";
    private final String PRODUCT_SALE_UPDATE_PAGE_GET_URL = "/partner/product/market_products/${saleId}/edit";
    private final String PRODUCT_SALE_UPDATE_POST_URL = "/partner/product/market_products/${saleId}";
    private final String PRODUCT_SALE_DESCRIPTION_IMAGE_POST_URL = "/ckeditor/pictures";
    private final String PRODUCT_SELLING_GET_URL = "/partner/product/market_products";
    private final String PRODUCT_SALE_STOP_POST_URL = "/partner/product/market_products/ajax_product_available_update";

    //QNA
    private final String QNA_REPLAY_POST_URL = "/partner/comments/update_comment_reply";
    private final String QNA_GET_URL = "/partner/comments";

    private final String PARTENER_NOTICE_GET_URL = "/partner/notice";

    //http://dev.reebonz.co.kr:3002/partner/comments?search_is_reply=ready
    //http://dev.reebonz.co.kr:3002/partner/comments?search_is_reply=totalcomment
    //http://dev.reebonz.co.kr:3002/partner/comments?search_is_reply=reopen
    //http://dev.reebonz.co.kr:3002/partner/comments?search_is_reply=complete
    //Order
    private final String ORDER_CONVERSATION_GET_URL = "/partner/qnas/${qnaId}";
    private final String ORDER_PROCESSING_LIST_GET_URL = "/partner/order/ordered_items/items_in_process";
    private final String ORDER_CLAIM_LIST_GET_URL = "/partner/order/ordered_items/req_item_list";
    private final String ORDER_PROCESSEED_LIST_GET_URL = "/partner/order/ordered_items/processed_items";

    private final String ORDER_PROCESSING_EXCEL_GET_URL = "/partner/order/ordered_items/items_in_process.csv";
    private final String ORDER_CLAIM_EXCEL_GET_URL = "/partner/order/ordered_items/req_item_list.csv";
    private final String ORDER_PROCESSEED_EXCEL_GET_URL = "/partner/order/ordered_items/processed_items.csv";

    private final String ORDER_CONFIRM_POST_URL = "/partner/order/ordered_items/partner_confirm_complete";
    private final String ORDER_SELL_CANCEL_POST_URL = "/partner/order/ordered_items/update_partner_comment";
    private final String ORDER_DELIVERY_POST_URL = "/partner/order/deliveries/create_or_update";
    private final String ORDER_RETURN_CONFIRM_POST_URL = "/partner/order/ordered_items/update_ordered_item";
    private final String ORDER_RETURN_COMPLETE_POST_URL = "/partner/order/ordered_items/update_ordered_item";
    private final String ORDER_RETURN_REJECT_POST_URL = "/partner/order/ordered_items/update_ordered_item";
    private final String ORDER_BUY_CANCEL_CONFIRM_POST_URL = "/partner/order/ordered_items/update_ordered_item";

    /**
     * 환경에 따라서 baseUrl을 가져온다
     * @return
     */
    private String getBaseUrl(){
        String activeProfile = System.getProperty("spring.profiles.active");
        if ("live".equals(activeProfile)) {
            return BASE_PARTNER_URL;
//            return DEV_BASE_URL;
        } else {
            return DEV_BASE_URL;
        }
    }


    /*
     * 로그인을 위한 데이타인 preToken과 쿠키값을 사인인 페이지를 통하여 가져온다.
     */
    public Connection.Response authenticityTokenAndCookie(Map<String, String> headers, Map<String, String> data) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + LOGIN_POST_URL, headers, data, null, false, false, false);
    }

    /*
     * 로그인에 대한 결과를 가져온다.
     * 이때 받아온 Cookie값을 세팅해서 전송해야 한다.
     */
    public Connection.Response login(Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + LOGIN_POST_URL,null, data, requestBody, cookies, false, false, false);
    }

    /*
     * 세션을 가지고 판매자 메인 대쉬보드 페이지를 접근하여 로그인 여부를 체크하기 위한 GET
     */
    public Connection.Response getDashboard(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + DASHBOARD_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * 상품 판매 등록
     */
    public Connection.Response postProductSale(
            Map<String, String> headers,
            Map<String, String> data,
            List<JsoupHttpClient.FormDataForInputStream> formDataForInputStreamList,
            Map<String, String> cookies) throws IOException
    {
        return JsoupHttpClient.post(
                getBaseUrl() + PRODUCT_SALE_POST_URL
                , headers
                , data
                , formDataForInputStreamList
                , null
                , cookies
                , true
                , false
                , false
        );
//        Connection connection = Jsoup.connect(getBaseUrl() + PRODUCT_SALE_POST_URL)
//                .method(Connection.Method.POST)
//                .headers(headers)
//                .cookies(cookies)
//                .ignoreContentType(true);
//
//        // 상품 이미지 폼데이터
//        for (ReebonzImageFileData reebonzImageFileData : reebonzImageFileDataList) {
//            connection.data(reebonzImageFileData.getKeyName(), reebonzImageFileData.getFileNameWithExtension(), reebonzImageFileData.getInputStream());
//        }
//        // 나머지 폼데이터
//        connection.data(data);
//
//        return connection.execute();
    }

    /*
     * 상품 수정 페이지 요청
     */
    public Connection.Response getProductSaleUpdatePage(Map<String, String> headers, Map<String, String> cookies, String saleId) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PRODUCT_SALE_UPDATE_PAGE_GET_URL.replace("${saleId}", saleId), headers, null, cookies,  false, false, true);
    }

    /*
     * 상품 판매 수정
     */
    public Connection.Response postProductSaleUpdate(
            Map<String, String> headers,
            Map<String, String> data,
            List<JsoupHttpClient.FormDataForInputStream> formDataForInputStreamList,
            Map<String, String> cookies,
            String saleId) throws IOException
    {
        return JsoupHttpClient.post(
                getBaseUrl() + PRODUCT_SALE_UPDATE_POST_URL.replace("${saleId}", saleId)
                , headers
                , data
                , formDataForInputStreamList
                , null
                , cookies
                , true
                , false
                , false
        );
    }

    /*
     * 상품 판매상태에 대한 판매중지 설정/해제 요청
     */
    public Connection.Response postProductSaleStop(
            Map<String, String> headers,
            Map<String, String> data,
            Map<String, String> cookies) throws IOException
    {
        return JsoupHttpClient.post(
                getBaseUrl() + PRODUCT_SALE_STOP_POST_URL
                , headers
                , data
                , null
                , cookies
                , true
                , false
                , false
        );
    }

    /*
     * 상품문의에 대한 답변을 기록한다.
     * 이때 받아온 Cookie값을 세팅해서 전송해야 한다.
     */
    public Connection.Response postQnAReply(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + QNA_REPLAY_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 세션을 가지고 상품문의 목록을 얻어온다.
     */
    public Connection.Response getQna(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + QNA_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * 주문대화에 대한 답변을 기록한다.
     * 이때 받아온 Cookie값을 세팅해서 전송해야 한다.
     */
    public Connection.Response postOrderConversationReply(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", ""), headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 세션을 가지고 주문대화 목록을 얻어온다.
     */
    public Connection.Response getOrderConversation(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", ""), headers, data, cookies,  false, false, true);
    }

    /*
     * 세션을 가지고 주문대화 상세를 얻어온다.
     */
    public Connection.Response getOrderConversationDetail(Map<String, String> headers, Map<String, String> cookies, String qnaId) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", qnaId), headers, null, cookies,  false, false, true);
    }

    /*
     * 세션을 가지고 진행주문 목록을 얻어온다.
     */
    public Connection.Response getOrder(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSING_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * 세션을 가지고 취소,반품요청 목록을 얻어온다.
     */
    public Connection.Response getOrderClaim(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CLAIM_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * 세션을 가지고 완료 주문 목록을 얻어온다.
     */
    public Connection.Response getOrderComplete(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSEED_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * 세션을 가지고 진행주문 목록을 얻어온다.
     */
    public Connection.Response getOrderByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSING_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * 세션을 가지고 취소,반품요청 목록을 얻어온다.
     */
    public Connection.Response getOrderClaimByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CLAIM_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * 세션을 가지고 완료 주문 목록을 얻어온다.
     */
    public Connection.Response getOrderCompleteByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSEED_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * 주문확인(배송준비중)
     */
    public Connection.Response postOrderConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 배송확인(송장입력,수정,배송중)
     */
    public Connection.Response postOrderDelivery(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_DELIVERY_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 반품확인(반품요청)
     */
    public Connection.Response postOrderReturnConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 반품완료(반품요청)
     */
    public Connection.Response postOrderReturnComplete(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_COMPLETE_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 반품거절(배송중)
     */
    public Connection.Response postOrderReturnReject(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_REJECT_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 품절,판매취소(배송중)
     */
    public Connection.Response postOrderSellCancel(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_SELL_CANCEL_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 구매취소요청 승인
     */
    public Connection.Response postOrderBuyCancelConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_BUY_CANCEL_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * 판매자 계정 로그인후 공지사항을 가져온다
     */
    public Connection.Response getPartenerNotice(Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PARTENER_NOTICE_GET_URL, null, null, cookies,  true, false, true);
    }

    /*
     * 상세설명에 삽입할 이미지 업로드
     */
    public Connection.Response postImageForSaleDescription(JsoupHttpClient.FormDataForInputStream formDataForInputStream) throws IOException {
        // 디폴트 키
        formDataForInputStream.setKeyName("upload");
        // 기본 폼 데이터
        HashMap<String, String> data = new HashMap<>();
        data.put("CKEditor", "product_text_description_attributes_content");
        data.put("CKEditorFuncNum", "138");

        return JsoupHttpClient.post(
                getBaseUrl() + PRODUCT_SALE_DESCRIPTION_IMAGE_POST_URL
                , null
                , data
                , Arrays.asList(formDataForInputStream)
                , null
                , null
                , true
                , false
                , false
        );
    }

    /*
     * 세션을 가지고 판매중 상품 정보를 얻어온다.
     */
    public Connection.Response getSellingProduct(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PRODUCT_SELLING_GET_URL, headers, data, cookies,  false, false, true);
    }

}
