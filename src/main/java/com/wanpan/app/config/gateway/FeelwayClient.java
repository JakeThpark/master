package com.wanpan.app.config.gateway;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class FeelwayClient {
    private static final String SESSION = "PHPSESSID";
    private static final String BASE_URL = "https://www.feelway.com";
    private static final String INIT_SESSION_URL = "/login.php";
    private static final String LOGIN_URL = "/login_ok.php";
    private static final String SIGN_IN_CHECKER_URL = "/mypage_seller_grade.php";
    private static final String NOTICE_LIST_GET_URL = "/board_notice.php?tab=2";
    private static final String NOTICE_DETAIL_GET_URL = "/board_notice_view.php";

    private static final String PRODUCT_REGISTER_FORM_URL = "/write_goods.php";
    private static final String PRODUCT_REGISTER_FORM_REFERER_URL = "/sell.php";
    private static final String PRODUCT_IMAGE_UPLOAD_URL = "/goods_file_up.php";
    private static final String PRODUCT_REGISTER_URL = "/save_goods.php";
    private static final String PRODUCT_REGISTER_REFERER_URL = PRODUCT_REGISTER_FORM_URL;
    private static final String PRODUCT_UPDATE_REFERER_URL = "/manager_goods.php";
    private static final String PRODUCT_DELETE_URL = "/save_goods.php";
    private static final String PRODUCT_DELETE_REFERER_URL = "/manager_goods.php";
    private static final String PRODUCT_SELLING_URL = "/mypage_sell_list.php";
    private static final String PRODUCT_ABSENCE_GOODS_URL = "/absence_goods_pop.php";


    private static final String ORDER_LIST_EXCEL_URL = "/mypage_sell_order_list_exceldown.php";
    private static final String ORDER_LIST_WEB_URL = "/mypage_seller.php";
    private static final String ORDER_CONVERSATION_MESSAGE_POST_URL = "/save_order_chat.php";
    private static final String ORDER_UPDATE_STATUS_POST_URL = "/mypage_order_handdle_seller.php";

    private static final String MYPAGE_URL = "/mypage.php";
    private static final String QNA_URL = "/mypage_goods_memo.php";
    private static final String QNA_ANSWER_POST_URL = "/memo_goods_iframe.php";


    public Connection.Response login(String session, Map<String, String> data) throws IOException {

        return Jsoup.connect(BASE_URL + LOGIN_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .data(data)
                .execute();
    }

    public Connection.Response getCookie() throws IOException {
        return Jsoup.connect(BASE_URL + INIT_SESSION_URL)
                .method(Connection.Method.POST)
                .execute();
    }

    public Connection.Response getForCheckingSignIn(String session) throws IOException {
        return Jsoup.connect(BASE_URL + SIGN_IN_CHECKER_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .execute();
    }

    public Connection.Response getRegisterProduct(String session, Map<String, String> data)
            throws IOException {
        return Jsoup.connect(BASE_URL + PRODUCT_REGISTER_FORM_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .referrer(BASE_URL + PRODUCT_REGISTER_FORM_REFERER_URL)
                .data(data)
                .execute();
    }

    public Connection.Response getUpdateProduct(String session, Map<String, String> data) throws IOException {
        return Jsoup.connect(BASE_URL + PRODUCT_REGISTER_FORM_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .referrer(BASE_URL + PRODUCT_UPDATE_REFERER_URL)
                .data(data)
                .execute();
    }


    public Connection.Response postImages(String session, Map<String, String> data,
                                          Map<String, InputStream> files) throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_IMAGE_UPLOAD_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            log.debug("postImages:{}", entry);
            connection.data(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, InputStream> entry : files.entrySet()) {
            connection.data(entry.getKey(), entry.getKey() + ".jpg", entry.getValue());
        }

        return connection.execute();
    }

    public Connection.Response registerProduct(String session, Map<String, String> data)
            throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_REGISTER_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .postDataCharset("euc-kr")
                .referrer(BASE_URL + PRODUCT_REGISTER_REFERER_URL);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String value = entry.getValue();
            if( value != null) {
                connection.data(entry.getKey(), value);
            }
        }

        return connection.execute();
    }

    public Connection.Response deleteProduct(String session, Map<String, String> data)
            throws IOException {

        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_DELETE_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .postDataCharset("euc-kr")
                .referrer(BASE_URL + PRODUCT_DELETE_REFERER_URL);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            connection.data(entry.getKey(), entry.getValue());
        }

        return connection.execute();
    }

    public Connection.Response getQnA(String session, Map<String, String> data)
            throws IOException {
        return Jsoup.connect(BASE_URL + QNA_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .referrer(BASE_URL + MYPAGE_URL)
                .data(data)
                .execute();
    }

    public Connection.Response getOrderFromExcel(String session, Map<String, String> data) throws IOException {
        Connection.Response xmlHttpRequest = Jsoup.connect(BASE_URL + ORDER_LIST_EXCEL_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .data(data)
                .header("X-Requested-With", "XMLHttpRequest")
                .execute();
        return xmlHttpRequest;
    }

    public Connection.Response getOrderFromPage(String session, Map<String, String> data) throws IOException {
        Connection.Response httpRequest = Jsoup.connect(BASE_URL + ORDER_LIST_WEB_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .referrer(BASE_URL + MYPAGE_URL)
                .data(data)
                .execute();
        return httpRequest;
    }
    
    public Connection.Response postAnswerForQna(String session, Map<String, String> data)
            throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + QNA_ANSWER_POST_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .postDataCharset("euc-kr")
                .referrer(BASE_URL + QNA_URL)
                .data(data);

        return connection.execute();
    }

    public Connection.Response postConversationMessageForOrder(String session, Map<String, String> data)
            throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + ORDER_CONVERSATION_MESSAGE_POST_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .postDataCharset("euc-kr")
                .referrer(BASE_URL + ORDER_LIST_WEB_URL)
                .data(data);

        return connection.execute();
    }

    public Connection.Response updateShopOrderByStatus(String session, Map<String, String> data)
            throws IOException {
        log.info("data:{}",data);
        Connection connection = Jsoup.connect(BASE_URL + ORDER_UPDATE_STATUS_POST_URL)
                .method(Connection.Method.POST)
                .cookie(SESSION, session)
                .postDataCharset("euc-kr")
                .referrer(BASE_URL + ORDER_LIST_WEB_URL)
                .data(data);

        return connection.execute();
    }

    public Connection.Response getShopNotice() throws IOException {
        return Jsoup.connect(BASE_URL + NOTICE_LIST_GET_URL)
                .method(Connection.Method.GET)
                .execute();
    }

    public Connection.Response getShopNoticeDetail(Map<String, String> data) throws IOException {
        return Jsoup.connect(BASE_URL + NOTICE_DETAIL_GET_URL)
                .method(Connection.Method.GET)
                .data(data)
                .execute();
    }

    public Connection.Response getSellingProduct(String session, Map<String, String> data) throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_SELLING_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session);
        if(!ObjectUtils.isEmpty(data)){
            connection.data(data);
        }

        return connection.execute();
    }

    public Connection.Response changeAbsenceProduct(String session, Map<String, String> data) throws IOException {
        Connection connection = Jsoup.connect(BASE_URL + PRODUCT_ABSENCE_GOODS_URL)
                .method(Connection.Method.GET)
                .cookie(SESSION, session)
                .data(data);

        return connection.execute();
    }


}
