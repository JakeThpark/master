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
     * ????????? ????????? baseUrl??? ????????????
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
     * ???????????? ?????? ???????????? preToken??? ???????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response authenticityTokenAndCookie(Map<String, String> headers, Map<String, String> data) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + LOGIN_POST_URL, headers, data, null, false, false, false);
    }

    /*
     * ???????????? ?????? ????????? ????????????.
     * ?????? ????????? Cookie?????? ???????????? ???????????? ??????.
     */
    public Connection.Response login(Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + LOGIN_POST_URL,null, data, requestBody, cookies, false, false, false);
    }

    /*
     * ????????? ????????? ????????? ?????? ???????????? ???????????? ???????????? ????????? ????????? ???????????? ?????? GET
     */
    public Connection.Response getDashboard(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + DASHBOARD_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * ?????? ?????? ??????
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
//        // ?????? ????????? ????????????
//        for (ReebonzImageFileData reebonzImageFileData : reebonzImageFileDataList) {
//            connection.data(reebonzImageFileData.getKeyName(), reebonzImageFileData.getFileNameWithExtension(), reebonzImageFileData.getInputStream());
//        }
//        // ????????? ????????????
//        connection.data(data);
//
//        return connection.execute();
    }

    /*
     * ?????? ?????? ????????? ??????
     */
    public Connection.Response getProductSaleUpdatePage(Map<String, String> headers, Map<String, String> cookies, String saleId) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PRODUCT_SALE_UPDATE_PAGE_GET_URL.replace("${saleId}", saleId), headers, null, cookies,  false, false, true);
    }

    /*
     * ?????? ?????? ??????
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
     * ?????? ??????????????? ?????? ???????????? ??????/?????? ??????
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
     * ??????????????? ?????? ????????? ????????????.
     * ?????? ????????? Cookie?????? ???????????? ???????????? ??????.
     */
    public Connection.Response postQnAReply(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + QNA_REPLAY_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response getQna(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + QNA_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * ??????????????? ?????? ????????? ????????????.
     * ?????? ????????? Cookie?????? ???????????? ???????????? ??????.
     */
    public Connection.Response postOrderConversationReply(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", ""), headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response getOrderConversation(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", ""), headers, data, cookies,  false, false, true);
    }

    /*
     * ????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response getOrderConversationDetail(Map<String, String> headers, Map<String, String> cookies, String qnaId) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CONVERSATION_GET_URL.replace("${qnaId}", qnaId), headers, null, cookies,  false, false, true);
    }

    /*
     * ????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response getOrder(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSING_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * ????????? ????????? ??????,???????????? ????????? ????????????.
     */
    public Connection.Response getOrderClaim(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CLAIM_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * ????????? ????????? ?????? ?????? ????????? ????????????.
     */
    public Connection.Response getOrderComplete(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSEED_LIST_GET_URL, headers, data, cookies,  false, false, true);
    }

    /*
     * ????????? ????????? ???????????? ????????? ????????????.
     */
    public Connection.Response getOrderByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSING_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * ????????? ????????? ??????,???????????? ????????? ????????????.
     */
    public Connection.Response getOrderClaimByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_CLAIM_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * ????????? ????????? ?????? ?????? ????????? ????????????.
     */
    public Connection.Response getOrderCompleteByExcel(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + ORDER_PROCESSEED_EXCEL_GET_URL, headers, data, cookies,  true, false, true);
    }

    /*
     * ????????????(???????????????)
     */
    public Connection.Response postOrderConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????????(????????????,??????,?????????)
     */
    public Connection.Response postOrderDelivery(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_DELIVERY_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????????(????????????)
     */
    public Connection.Response postOrderReturnConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????????(????????????)
     */
    public Connection.Response postOrderReturnComplete(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_COMPLETE_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????????(?????????)
     */
    public Connection.Response postOrderReturnReject(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_RETURN_REJECT_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ??????,????????????(?????????)
     */
    public Connection.Response postOrderSellCancel(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_SELL_CANCEL_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ?????????????????? ??????
     */
    public Connection.Response postOrderBuyCancelConfirm(Map<String, String> headers, Map<String, String> data, String requestBody, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.post(getBaseUrl() + ORDER_BUY_CANCEL_CONFIRM_POST_URL,headers, data, requestBody, cookies, true, false, true);
    }

    /*
     * ????????? ?????? ???????????? ??????????????? ????????????
     */
    public Connection.Response getPartenerNotice(Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PARTENER_NOTICE_GET_URL, null, null, cookies,  true, false, true);
    }

    /*
     * ??????????????? ????????? ????????? ?????????
     */
    public Connection.Response postImageForSaleDescription(JsoupHttpClient.FormDataForInputStream formDataForInputStream) throws IOException {
        // ????????? ???
        formDataForInputStream.setKeyName("upload");
        // ?????? ??? ?????????
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
     * ????????? ????????? ????????? ?????? ????????? ????????????.
     */
    public Connection.Response getSellingProduct(Map<String, String> headers, Map<String, String> data, Map<String, String> cookies) throws IOException {

        return JsoupHttpClient.get(getBaseUrl() + PRODUCT_SELLING_GET_URL, headers, data, cookies,  false, false, true);
    }

}
