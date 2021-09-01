package com.wanpan.app.service.reebonz;

import com.wanpan.app.config.JsoupHttpClient;
import com.wanpan.app.config.gateway.ReebonzHttpClient;
import com.wanpan.app.dto.job.*;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.dto.reebonz.ReebonzImageFileData;
import com.wanpan.app.dto.reebonz.ReebonzProductOptionStock;
import com.wanpan.app.dto.reebonz.ReebonzWebPageProductUpdate;
import com.wanpan.app.service.reebonz.constant.ReebonzOrderListType;
import com.wanpan.app.service.reebonz.constant.ReebonzProductCondition;
import com.wanpan.app.service.reebonz.constant.ReebonzSaleStatus;
import com.wanpan.app.service.reebonz.parser.ReebonzOrderParser;
import com.wanpan.app.service.reebonz.parser.ReebonzSaleParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ReebonzWebPageService {
    private final ReebonzHttpClient reebonzHttpClient;
    private final ReebonzSaleParser reebonzSaleParser;
    private final ModelMapper modelMapper;

    private final String SESSION_NAME = "_cv2rb_session";
    private final String CSRF_TOKEN_HEADER = "X-CSRF-Token";

    /*
     * 리본즈 웹 로그인을 위해서 쿠키에 들어가는 session과 페이지에 존재하는 authenticityToken를 받아온다.
     */
    public PreLoginData getPreLoginData() throws IOException {
        log.info("CALL getPreLoginData");
        Connection.Response response = reebonzHttpClient.authenticityTokenAndCookie(null,null);
//        log.info("getPreLoginData response: {}",response.body());

        //<meta content="zavkKj6P5LOL3ij2mLcPTvNinNiv71HPQUNLejncTjA=" name="csrf-token" />
        String authenticityToken = "";
        Document document = Jsoup.parse(response.body());
        Elements elements = document.getElementsByTag("meta");
        for(Element element : elements){
            if(element.attr("name").equals("csrf-token")){
                authenticityToken = element.attr("content");
            }
        }
        log.info("getPreLoginData authenticityToken:{}", authenticityToken);

        return new PreLoginData(authenticityToken, response.cookies());
    }

    /*
     * preData에서 cookie와 authenticityToken을 가지고 로그인을 시도해서 cookie에 있는 session을 로그인된 유효한 session으로 만든다.
     */
    public String getToken(String accountId, String password) throws IOException {
        log.info("CALL getToken");
        PreLoginData preLoginData = getPreLoginData();
        //signin에 필요한 form data를 구성한다.
        Map<String, String> signInFormMap = new HashMap<>();
        signInFormMap.put("user[login]",accountId);
        signInFormMap.put("user[password]",password);
        signInFormMap.put("user[remember_me]","0");
        signInFormMap.put("authenticity_token",preLoginData.getAuthenticityToken());

        Connection.Response postResponse = reebonzHttpClient.login(signInFormMap, null, preLoginData.getCookies());
//        log.info("response body: {}", postResponse.body());
        log.info("response cookies: {}", postResponse.cookies());

        String webToken = null;
        if(postResponse.hasCookie(SESSION_NAME)) {
            webToken = postResponse.cookie(SESSION_NAME);
        }

        //Login Check
        if(!StringUtils.isEmpty(webToken) && !isKeepSignIn(webToken)){
            webToken = null;
        }

        return webToken;
    }

    /*
     * 메인 페이지를 호출함으로써 로그인 여부를 판단한다.
     */
    public boolean isKeepSignIn(String webToken) throws IOException {
//        log.info("isValidSession session: {}", webToken);
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Connection.Response response = reebonzHttpClient.getDashboard(null,null, cookies);
//        log.info("response.headers: {}", response.headers());
        if(response.body().indexOf("로그아웃") > 0){
            log.info("Reebonz WebToken check - Success!!");
            return true;
        }else{
            log.info("Reebonz WebToken check - Fail!!");
            return false;
        }
    }

    /**
     *
     * @param webToken 로그인 세션 토큰
     * @return csrf_token
     * @throws IOException IOException
     */
    public String getCsrfTokenByWebToken(String webToken) throws IOException {
//        log.info("isValidSession session: {}", webToken);
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Connection.Response response = reebonzHttpClient.getDashboard(null,null, cookies);
//        log.info("response.headers: {}", response.headers());
        if(response.body().indexOf("로그아웃") > 0){
            log.info("Reebonz WebToken check - Success!!");
            String authenticityToken = "";
            Document document = Jsoup.parse(response.body());
            Elements elements = document.getElementsByTag("meta");
            for(Element element : elements){
                if(element.attr("name").equals("csrf-token")){
                    authenticityToken = element.attr("content");
                }
            }
            log.info("getCsrfTokenByWebToken authenticityToken:{}", authenticityToken);
            return authenticityToken;
        }else{
            log.info("Reebonz WebToken check - Fail!!");
            return null;
        }
    }

    /*
     * 검색 타입에 따라서 Qna html을 가져온다.
     */
    public String collectQna(String webToken, String paramSearchIsReply) throws IOException {
        log.info("Call collectQna");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("search_is_reply", paramSearchIsReply);

        Connection.Response getResponse = reebonzHttpClient.getQna(null, data, cookies);

        return getResponse.body();
    }

    /*
     * 검색어에 따라서 Qna html을 가져온다.
     */
    //http://dev.reebonz.co.kr:3007/partner/comments?utf8=%E2%9C%93&search_product_name=TODS&search_text=%E3%85%85%E3%84%B3%E3%84%B3%E3%84%B3&commit=%EA%B2%80%EC%83%89
    public String collectQnaBySearchCondition(String webToken, String searchProductName, String questionContent) throws IOException {
        log.info("Call collectQna");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("utf8", URLEncoder.encode("%E2%9C%93", StandardCharsets.UTF_8) );
        data.put("search_product_name", searchProductName); //product_subject
        data.put("search_text", questionContent); //question_content
        data.put("commit", "검색");

        Connection.Response getResponse = reebonzHttpClient.getQna(null, data, cookies);

        return getResponse.body();
    }

    /**
     * 상품문의에 대한 답변을 POST 한다.
     * @param webToken
     * @param postJobDto
     * @return
     * @throws IOException
     */
    public String postAnswerForQna(String webToken, ShopQnaJobDto.Request.PostJob postJobDto) throws IOException {
        log.info("Call postAnswerForQnaToShop");
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
        headers.put("Referer","http://dev.reebonz.co.kr:3002/partner/comments?search_is_reply=totalcomment");
        headers.put("Host","dev.reebonz.co.kr:3002");
        headers.put("Origin","http://dev.reebonz.co.kr:3002");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> postQnaReplyFormMap = new HashMap<>();
        postQnaReplyFormMap.put("comment_id",postJobDto.getShopQna().getQuestionId()); //문의번호
        postQnaReplyFormMap.put("comment",postJobDto.getShopQna().getShopQnaConversation().getContent()); //답변내용

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postQnAReply(headers, postQnaReplyFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());
        //{"result":"success","message":null,"data":{"user_id":413178,"reply_id":553642,"content":"1111","created_at":"2020. 07. 23 16:39:30"}}

        return postResponse.body();
    }


    /**
     * 검색 타입에 따라서 Order Conversation html을 가져온다.
     */
    public String collectOrderConversation(String webToken, String paramQnAStatus, int pageNumber) throws IOException {
//        log.info("Call collectOrderConversation()");

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME, webToken);

        Map<String, String> data = new HashMap<>();
        data.put("qna_status", paramQnAStatus);
        data.put("page", String.valueOf(pageNumber));

        Connection.Response getResponse = reebonzHttpClient.getOrderConversation(null, data, cookies);

        return getResponse.body();
    }

    /**
     * Order Conversation detail html을 가져온다.
     */
    public String collectOrderConversationDetail(String webToken, String qnaId) throws IOException {
//        log.info("Call collectOrderConversationDetail()");

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME, webToken);

        Connection.Response orderConversationDetailResponse = reebonzHttpClient.getOrderConversationDetail(null, cookies, qnaId);

        return orderConversationDetailResponse.body();
    }

    /**
     * 주문대화에 대한 답변을 등록한다.
     */
    public String postOrderConversationReply(String webToken, OrderJobDto.Request.PostConversationJob postJobDto) throws IOException {
        log.info("Call postOrderConversationReply()");

        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER, csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> formData = new HashMap<>();
        formData.put("qna[parent_id]", postJobDto.getChannelId()); // 채널ID
        formData.put("qna[from]", "0");
        formData.put("view", "show");
        formData.put("qna[content]", postJobDto.getOrderConversationMessage()); // 내용

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderConversationReply(headers, formData, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());

        return postResponse.body();
    }

    /*
     * 엑셀 주문 수집(진행주문 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderProcessingFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderProcessingFromExcel(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderProcessingFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderProcessingByExcel");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrderByExcel(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromExcelHtml(getResponse.body());
    }

    /*
     * 엑셀 주문 수집(취소,반품요청 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderClaimFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderClaimFromExcel(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderClaimFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderClaimByExcel");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrderClaimByExcel(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromExcelHtml(getResponse.body());
    }

    /*
     * 엑셀 주문 수집(완료주문 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderCompleteFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderCompleteFromExcel(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderCompleteFromExcel(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderCompleteByExcel");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrderCompleteByExcel(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromExcelHtml(getResponse.body());
    }

    /*
     * 웹 주문 수집(진행주문 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderProcessingFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderProcessingFromWeb(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderProcessingFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderProcessingByWeb");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrder(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromWebPage(getResponse.body(), false);
    }

    /*
     * 웹 주문 수집(취소,반품요청 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderClaimFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderClaimFromWeb(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderClaimFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderClaimByWeb");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrderClaim(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromWebPage(getResponse.body(), false);
    }

    /*
     * 주문 수집(완료주문 메뉴)
     */
    public List<OrderDto.Request.CollectCallback> collectOrderCompleteFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType) throws IOException {
        return collectOrderCompleteFromWeb(webToken, reebonzOrderListType, null);
    }

    public List<OrderDto.Request.CollectCallback> collectOrderCompleteFromWeb(String webToken, ReebonzOrderListType reebonzOrderListType, String orderId) throws IOException {
        log.info("Call collectOrderCompleteByWeb");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("list_type", reebonzOrderListType.getCode());
        if(!StringUtils.isEmpty(orderId)){
            data.put("search_order_number", orderId);
        }

        Connection.Response getResponse = reebonzHttpClient.getOrderComplete(null, data, cookies);

        return ReebonzOrderParser.parseOrderListFromWebPage(getResponse.body(), true);
    }

    /**
     * 주문상태 변경 - 배송준비중
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderConfirm(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderConfirm");
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/items_in_process");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderConfirmFormMap = new HashMap<>();
        updateOrderConfirmFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderConfirm(headers, updateOrderConfirmFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());
        //{"result":"success","message":null,"data":{"user_id":413178,"reply_id":553642,"content":"1111","created_at":"2020. 07. 23 16:39:30"}}

        return postResponse.body();
    }

    /**
     * 주문상태 변경 - 배송중
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderDelivery(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderDelivery");
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/items_in_process");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderConfirmFormMap = new HashMap<>();
        updateOrderConfirmFormMap.put("utf8",URLEncoder.encode("%E2%9C%93", StandardCharsets.UTF_8));
        updateOrderConfirmFormMap.put("authenticity_token",csrfToken); //(X-CSRF-Token)
        updateOrderConfirmFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderConfirmFormMap.put("request_url","partner");
        updateOrderConfirmFormMap.put("state","normal");
        updateOrderConfirmFormMap.put("delivery_method_id",updateJob.getCourier().getCode()); //리본즈 샵 기준 택배사 코드
        updateOrderConfirmFormMap.put("tracking_code",updateJob.getTrackingNumber()); //송장번호

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderDelivery(headers, updateOrderConfirmFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());
        //{"result":"success","message":null,"data":{"user_id":413178,"reply_id":553642,"content":"1111","created_at":"2020. 07. 23 16:39:30"}}

        return postResponse.body();
    }

    /**
     * 주문상태 변경 - 반품확인
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderReturnConfirm(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderReturnConfirm");
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/req_item_list");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderReturnConfirmFormMap = new HashMap<>();
        updateOrderReturnConfirmFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderReturnConfirmFormMap.put("save_type","refund_confirm");
        //반품 확인 메세지가 별도로 없을 경우 Default 메세지를 작성한다.
        if(StringUtils.isEmpty(updateJob.getSellerMessage())){
            updateOrderReturnConfirmFormMap.put("comment","반품 진행을 위해 상품을 보내주세요.");
        }else{
            updateOrderReturnConfirmFormMap.put("comment",updateJob.getSellerMessage());
        }
        log.info("updateOrderReturnConfirmFormMap: {}", updateOrderReturnConfirmFormMap);
        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderReturnConfirm(headers, updateOrderReturnConfirmFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());
        //{"result":"success","message":null,"data":{"user_id":413178,"reply_id":553642,"content":"1111","created_at":"2020. 07. 23 16:39:30"}}

        return postResponse.body();
    }

    /**
     * 주문상태 변경 - 반품완료
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderReturnComplete(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderReturnComplete");
        log.info("{}",updateJob);
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
//        headers.put("Content-Type","multipart/form-data");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/req_item_list");
        headers.put("Host","partner.reebonz.co.kr"); //필수값
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderReturnCompleteFormMap = new HashMap<>();
        updateOrderReturnCompleteFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderReturnCompleteFormMap.put("save_type","refund_true");
        //반품 확인 메세지가 별도로 없을 경우 Default 메세지를 작성한다.
        if(StringUtils.isEmpty(updateJob.getSellerMessage())){
            updateOrderReturnCompleteFormMap.put("comment","반품 상품을 수령하였습니다. 결제 취소가 곧 이루어질 예정이며, 카드사 사정에 의해 영업일 1~2일 정도가 소요될 수 있습니다.");
        }else{
            updateOrderReturnCompleteFormMap.put("comment",updateJob.getSellerMessage());
        }

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderReturnComplete(headers, updateOrderReturnCompleteFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());

        return postResponse.body();
    }

    /**
     * 주문상태 변경 - 반품거절
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderReturnReject(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderReturnReject");
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/req_item_list");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderReturnRejectFormMap = new HashMap<>();
        updateOrderReturnRejectFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderReturnRejectFormMap.put("save_type","refund_refuse");
        //반품 확인 메세지가 별도로 없을 경우 Default 메세지를 작성한다.
        if(StringUtils.isEmpty(updateJob.getSellerMessage())){
            updateOrderReturnRejectFormMap.put("comment","테스트중입니다.");
        }else{
            updateOrderReturnRejectFormMap.put("comment",updateJob.getSellerMessage());
        }

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderReturnReject(headers, updateOrderReturnRejectFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());

        return postResponse.body();
    }

    /**
     * 주문상태 변경 - 판매취소, 품절처리
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderSellCancel(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderSellCancel");
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/req_item_list");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderFormMap = new HashMap<>();
        updateOrderFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderFormMap.put("request_type","soldout");

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderSellCancel(headers, updateOrderFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());

        return postResponse.body();
    }

    /**
     * 취소요청 승인
     * @param webToken
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderBuyCancelConfirm(String webToken, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateOrderBuyCancelConfirm");
        log.info("{}",updateJob);
        log.info("webToken: {}",webToken);
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json, text/javascript, */*");
        headers.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
//        headers.put("Content-Type","multipart/form-data");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/order/ordered_items/req_item_list");
        headers.put("Host","partner.reebonz.co.kr"); //필수값
        headers.put("Origin","https://partner.reebonz.co.kr");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER,csrfToken);

        //post에 필요한 form data를 구성한다.
        Map<String, String> updateOrderReturnCompleteFormMap = new HashMap<>();
        updateOrderReturnCompleteFormMap.put("ordered_item_id",updateJob.getShopUniqueOrderId()); //주문 상세 번호(404343)
        updateOrderReturnCompleteFormMap.put("save_type","cancel_confirm");

        //Http call
        Connection.Response postResponse = reebonzHttpClient.postOrderBuyCancelConfirm(headers, updateOrderReturnCompleteFormMap, null, cookies);
        log.info("postResponse.body() - {}", postResponse.body());

        return postResponse.body();
    }

    /**
     * 상품 판매 등록
     * @param webToken
     * @param onlineSaleDto
     * @return
     */
    public String postProductSale(String webToken, OnlineSaleDto onlineSaleDto) throws IOException {
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("Reebonz postProductSale csrfToken={}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("Reebonz postProductSale get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME, webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","*/*");
//        headers.put("Content-Type","multipart/form-data"); // 상품 이미지 업로드 오류 문제로 인해 주석 처리함
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer", "https://partner.reebonz.co.kr/partner/product/market_products/new");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr/partner/product/market_products");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER, csrfToken);

        // 상품 이미지 파일 데이터 세팅
        List<ReebonzImageFileData> reebonzImageFileDataList = createProductImageFormData(onlineSaleDto.getSaleImageList());

        // 상세설명 관련 이미지 업로드를 하고 URL을 받아온다.
        List<String> uploadedImageUrlList = postImageListForSaleDescription(reebonzImageFileDataList);

        //업로드에서 이미지 스트림 사용에 의해서 다시 스트림을 연다
        reebonzImageFileDataList = createProductImageFormData(onlineSaleDto.getSaleImageList());
        
        // 폼 데이터를 세팅한다.
        Map<String, String> postFormData = mapToProductSalePostFormData(csrfToken, uploadedImageUrlList, onlineSaleDto, null); // 나머지 폼 데이터 세팅

        //공통 jsoupclient를 사용하기 위한 매핑
        List<JsoupHttpClient.FormDataForInputStream> formDataForInputStreamList =
                modelMapper.map(reebonzImageFileDataList, new TypeToken<List<JsoupHttpClient.FormDataForInputStream>>() {}.getType());

        log.info("=========Before Reebonz postProductSale Form =>\n{}", postFormData);
        Connection.Response response = reebonzHttpClient.postProductSale(headers, postFormData, formDataForInputStreamList, cookies);
        String responseBody = response.body();
        log.info("=========After Reebonz postProductSale Response Body =>\n{}", responseBody);

        return responseBody;
    }

    /**
     * 상품 판매 수정
     * @param webToken
     * @param onlineSaleDto
     * @return
     */
    public String postProductSaleUpdate(String webToken, OnlineSaleDto onlineSaleDto) throws IOException {
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("Reebonz postProductSaleUpdate csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("Reebonz postProductSaleUpdate get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME, webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","*/*");
//        headers.put("Content-Type","multipart/form-data"); // 상품 이미지 업로드 오류 문제로 인해 주석 처리함
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/product/market_products/edit");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr/partner/product/market_products");
        headers.put("X-Requested-With","XMLHttpRequest");

        //Get을 제외한 요청시 CSRF_TOKEN_HEADER 반드시 일치해야 하며 맞지 않을시 로그아웃처리됨
        headers.put(CSRF_TOKEN_HEADER, csrfToken);

        String saleId = onlineSaleDto.getShopSale().getPostId();

        // 쇼핑몰로 부터 기존 판매글 관련 업데이트 대상 내용 가져오기
        Connection.Response getPageResponse = reebonzHttpClient.getProductSaleUpdatePage(headers, cookies, saleId);
        ReebonzWebPageProductUpdate.ProductInfoUpdateTarget productInfoUpdateTarget = ReebonzOrderParser.parseProductInfoClearFromWebPage(getPageResponse.body());

        // 상품 이미지 파일 데이터 세팅
        List<ReebonzImageFileData> reebonzImageFileDataList = createProductImageFormData(onlineSaleDto.getSaleImageList());

        // 상세설명 관련 이미지 업로드를 하고 URL을 받아온다.
        List<String> uploadedImageUrlList = postImageListForSaleDescription(reebonzImageFileDataList);

        //업로드에서 이미지 스트림 사용에 의해서 다시 스트림을 연다
        reebonzImageFileDataList = createProductImageFormData(onlineSaleDto.getSaleImageList());

        // 폼 데이터 세팅
        Map<String, String> postFormData = mapToProductSalePostFormData(webToken, uploadedImageUrlList, onlineSaleDto, productInfoUpdateTarget); // 나머지 폼 데이터 세팅

        //공통 jsoupclient를 사용하기 위한 매핑
        List<JsoupHttpClient.FormDataForInputStream> formDataForInputStreamList = new ArrayList<>();
        modelMapper.map(reebonzImageFileDataList, formDataForInputStreamList);


        log.info("=========Before Reebonz postProductSaleUpdate Form =>\n{}", postFormData);
        Connection.Response response = reebonzHttpClient.postProductSaleUpdate(headers, postFormData, formDataForInputStreamList, cookies, saleId);
        String responseBody = response.body();
        log.info("=========After Reebonz postProductSaleUpdate Response Body =>\n{}", responseBody);

        return responseBody;
    }

    /**
     * 판매글 내용에 포함시킬 상품 이미지 폼 데이터를 생성한다
     */
    private List<ReebonzImageFileData> createProductImageFormData(List<OnlineSaleImageDto> onlineSaleImageList) throws IOException {
        if (onlineSaleImageList.isEmpty()) {
            throw new IOException("상품 이미지 필요");
        }

        onlineSaleImageList.sort(new ReebonzSaleImageListSort()); // 대표 이미지가 첫번째 순서로 오게 정렬
        List<ReebonzImageFileData> reebonzImageFileDataList = new ArrayList<>();

        for (int i = 0; i < onlineSaleImageList.size(); i++) {
            OnlineSaleImageDto onlineSaleImage = onlineSaleImageList.get(i);
            String originImagePath = String.valueOf(onlineSaleImage.getOriginImagePath());
            String[] splitImageUrl = originImagePath.split("/");
            String fileNameWithExtension = splitImageUrl[splitImageUrl.length - 1];
            String[] splitFileNameWithExtension = fileNameWithExtension.split("\\.");
            String extension = splitFileNameWithExtension[splitFileNameWithExtension.length - 1];
            InputStream inputStream = getImageInputStream(originImagePath, extension);

            String keyName = onlineSaleImage.isMainFlag() ?
                    String.format("product[img_representative_attributes][new_%d][filename]", i) :
                    String.format("product[img_details_attributes][new_%d][filename]", i);

            reebonzImageFileDataList.add(
                    new ReebonzImageFileData(keyName, fileNameWithExtension, inputStream));
        }

        return reebonzImageFileDataList;
    }

    /**
     * 판매 등록/수정을 위한 기본 폼 데이터를 세팅한다
     */
    private Map<String, String> mapToProductSalePostFormData(String csrfToken, List<String> uploadedImageUrlList, OnlineSaleDto onlineSaleDto, ReebonzWebPageProductUpdate.ProductInfoUpdateTarget productInfoUpdateTarget) throws IOException {
        Map<String, String> formData = new HashMap<>();
        List<ReebonzProductOptionStock> formDataReebonzProductOptionStockList = new ArrayList<>(); // 폼 데이터로 처리되는 상품옵션 리스트
        List<ReebonzProductOptionStock> jobProductOptionStockList = new ArrayList<>(); // 요청 job 관련 상품옵션 리스트
        Map<String, ReebonzProductOptionStock> jobProductOptionMap = new HashMap<>(); // 요청 job 관련 상품옵션 맵

        // 요청 job 관련 옵션 내용
        for (ProductDto product : onlineSaleDto.getProductList()) {
            for (ProductOptionDto productOption : product.getProductOptionList()) {
                ReebonzProductOptionStock jobReebonzProductOptionStock = new ReebonzProductOptionStock();
                // 옵션명 - "{분류값}|{수량}" 형식으로 세팅
                jobReebonzProductOptionStock.setOptionName(
                        product.getClassificationValue() + "|" + productOption.getName());
                // 옵션수량
                jobReebonzProductOptionStock.setOptionQuantity(String.valueOf(productOption.getQuantity()));
                // 리스트에 추가
                jobProductOptionStockList.add(jobReebonzProductOptionStock);
                // 맵에 추가
                jobProductOptionMap.put(jobReebonzProductOptionStock.getOptionName(), jobReebonzProductOptionStock);
            }
        }

        // 기존 판매글의 업데이트 대상 항목에 대한 처리
        if (productInfoUpdateTarget != null) { // 판매글 업데이트 작업인 경우
            // 판매 수정 플래그
            formData.put("_method", "put");

            /*
            상품옵션 관련 작업
             */
            // 기존 판매글 관련 상품옵션 정보
            List<ReebonzProductOptionStock> currentSaleProductOptionUpdateTargetList = productInfoUpdateTarget.getProductOptionUpdateTargetList(); // 기존 판매글 관련 상품옵션 리스트
            Map<String, ReebonzProductOptionStock> currentSaleProductOptionMap = new HashMap<>(); // 기존 판매글 관련 상품옵션 맵
            for (ReebonzProductOptionStock currentSaleReebonzProductOptionStock : currentSaleProductOptionUpdateTargetList) {
                // 맵에 추가
                currentSaleProductOptionMap.put(currentSaleReebonzProductOptionStock.getOptionName(), currentSaleReebonzProductOptionStock);
            }

            // 기존 판매글의 상품옵션 업데이트 정보를 폼 데이터 리스트에 추가
            for (ReebonzProductOptionStock currentSaleProductOptionUpdateTarget : currentSaleProductOptionUpdateTargetList) {
                ReebonzProductOptionStock formDataReebonzProductOptionStock = new ReebonzProductOptionStock();
                formDataReebonzProductOptionStock.setOptionId(
                        currentSaleProductOptionUpdateTarget.getOptionId());
                formDataReebonzProductOptionStock.setOptionName(
                        currentSaleProductOptionUpdateTarget.getOptionName());
                formDataReebonzProductOptionStock.setOptionQuantity(
                        currentSaleProductOptionUpdateTarget.getOptionQuantity());
                formDataReebonzProductOptionStock.setOptionAvailableFlag(
                        currentSaleProductOptionUpdateTarget.getOptionAvailableFlag());

                String optionName = currentSaleProductOptionUpdateTarget.getOptionName();
                if (jobProductOptionMap.containsKey(optionName)) {
                    // 수량 변경
                    formDataReebonzProductOptionStock.setOptionQuantity(
                            jobProductOptionMap.get(optionName).getOptionQuantity());
                    // 노출되게 변경
                    formDataReebonzProductOptionStock.setOptionAvailableFlag("true");
                } else {
                    // 노출되지 않게 변경
                    formDataReebonzProductOptionStock.setOptionAvailableFlag("false");
                }

                // 리스트에 추가
                formDataReebonzProductOptionStockList.add(formDataReebonzProductOptionStock);
            }

            // 요청 job 관련 상품옵션 중 신규건인 경우에만 폼 데이터 리스트에 추가
            for (ReebonzProductOptionStock jobProductOptionStock : jobProductOptionStockList) {
                String optionName = jobProductOptionStock.getOptionName();
                if (!currentSaleProductOptionMap.containsKey(optionName)) {
                    ReebonzProductOptionStock formDataReebonzProductOptionStock = new ReebonzProductOptionStock();
                    formDataReebonzProductOptionStock.setOptionName(
                            jobProductOptionStock.getOptionName());
                    formDataReebonzProductOptionStock.setOptionQuantity(
                            jobProductOptionStock.getOptionQuantity());
                    formDataReebonzProductOptionStock.setOptionAvailableFlag("true");

                    // 리스트에 추가
                    formDataReebonzProductOptionStockList.add(formDataReebonzProductOptionStock);
                }
            }

            /*
            상품 이미지 초기화 폼 데이터 세팅
             */
            if (productInfoUpdateTarget.getProductImageClearTargetList().size() > 0) {
                List<ReebonzWebPageProductUpdate.ProductImageClearTarget> productImageClearTargetList = productInfoUpdateTarget.getProductImageClearTargetList();
                for (int i = 0; i < productImageClearTargetList.size(); i++) {
                    ReebonzWebPageProductUpdate.ProductImageClearTarget productImageClearTarget = productImageClearTargetList.get(i);
                    int imageIdx = i * 2;

                    // 상품 이미지 ID
                    formData.put(
                            String.format("product[destroy_images][%d][id]", imageIdx),
                            productImageClearTarget.getId()
                    );

                    // 상품 이미지 타입
                    formData.put(
                            String.format("product[destroy_images][%d][type]", imageIdx),
                            productImageClearTarget.getType()
                    );
                }
            }
        } else { // 판매글 신규등록 작업인 경우
            /*
            상품 옵션 추가 정보를 폼 데이터 리스트에 추가
             */
            for (ReebonzProductOptionStock jobProductOptionStock : jobProductOptionStockList) {
                ReebonzProductOptionStock formDataReebonzProductOptionStock = new ReebonzProductOptionStock();
                formDataReebonzProductOptionStock.setOptionName(
                        jobProductOptionStock.getOptionName());
                formDataReebonzProductOptionStock.setOptionQuantity(
                        jobProductOptionStock.getOptionQuantity());
                formDataReebonzProductOptionStock.setOptionAvailableFlag("true");

                // 리스트에 추가
                formDataReebonzProductOptionStockList.add(jobProductOptionStock);
            }
        }


        for (int i = 0; i < formDataReebonzProductOptionStockList.size(); i++) {
            ReebonzProductOptionStock reebonzProductOptionStock = formDataReebonzProductOptionStockList.get(i);
            String parameterNamePrefix = String.format("product[stocks][%d]", i);

            formData.put(parameterNamePrefix + "[name]", reebonzProductOptionStock.getOptionName()); // 옵션명
            formData.put(parameterNamePrefix + "[stock_count]", reebonzProductOptionStock.getOptionQuantity()); // 옵션 수량

            if (!StringUtils.isEmpty(reebonzProductOptionStock.getOptionId())) {
                formData.put(parameterNamePrefix + "[id]", reebonzProductOptionStock.getOptionId());
                formData.put(parameterNamePrefix + "[available]", reebonzProductOptionStock.getOptionAvailableFlag());
            }
        }

        // 토큰
        formData.put("authenticity_token", csrfToken);
        // 브랜드명
        formData.put("brand_name", onlineSaleDto.getBrandMap().getSourceName().toUpperCase());
        // 상품명(한글)
        formData.put("product_meta_info[korea_name]", onlineSaleDto.getSubject());
        // 상품명(영문)
        formData.put("product_meta_info[name]", onlineSaleDto.getSubject());
        // 상품명(직접입력)
        formData.put("product_self_name", onlineSaleDto.getSubject());
        // 대표 소재
        formData.put("material_representative", onlineSaleDto.getMaterial());
        // 대표 색상
        formData.put("color_representative", onlineSaleDto.getColor());
        // 제조국(원산지)
        formData.put("product_meta_info[country]", onlineSaleDto.getProductionCountry());
        // 시즌 연도
        formData.put("season_year", onlineSaleDto.getSeasonYear());
        // 시즌 계절
        formData.put("season_type", onlineSaleDto.getSeason());
        // 상품 상태
        formData.put("product_type",onlineSaleDto.getCondition() == OnlineSaleDto.ProductCondition.UNUSED ? ReebonzProductCondition.NEW.name() : ReebonzProductCondition.VINTAGE.name());
        // 판매가격
        formData.put("product[selling_price]", String.valueOf(onlineSaleDto.getPrice()));
        // 사이즈 정보
        formData.put("product[text_size_info_attributes][content]", "");
        // 상세설명
        String saleDescriptionWithImage = appendImageListToSaleDescription(onlineSaleDto.getDetail().replace("\n", "<br>"), uploadedImageUrlList);
        formData.put("product[text_description_attributes][content]", saleDescriptionWithImage);
        // 취급 유의사항
        formData.put("product[text_tip_attributes][content]", "");
        // 구매 대행 여부
        formData.put("product[is_substitution]", "false");
        // 직구 여부
        formData.put("product[is_direct_buy]", "false");
        // 사이즈 존재여부
        formData.put("has_size_option", "true");
        // 법정 카테고리 체크박스
        formData.put("legal-info", "3");
        // 법정 카테고리 내용
        formData.put("product[text_legal_info_attributes][content]", createProductInformationNoticeText(onlineSaleDto));

        /*
        카테고리 정보
         */
        ShopCategoryDto genderCategory = onlineSaleDto.getShopSale().getShopCategory();
        ShopCategoryDto largeCategory = genderCategory.getChild();
        ShopCategoryDto mediumCategory = largeCategory.getChild();
        ShopCategoryDto smallCategory = mediumCategory.getChild();
        formData.put("category_gender", genderCategory.getDescription()); // 성별 카테고리
        formData.put("product_meta_info[category_ids]",
                String.format("%s,%s,%s",
                        largeCategory.getShopCategoryCode(),
                        mediumCategory.getShopCategoryCode(),
                        smallCategory.getShopCategoryCode()));  // 대중소 카테고리

        return formData;
    }

    /**
     * 이미지 파일에 대한 InputStream를 구한다
     */
    private InputStream getImageInputStream(String imageUrl, String extension) throws IOException {
        BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, extension, outputStream);
        outputStream.flush();
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return new ByteArrayInputStream(bytes);
    }

    public String collectShopNotice(String webToken) throws IOException {
        log.info("Call collectShopNotice");

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        return reebonzHttpClient.getPartenerNotice(cookies).body();
    }

//    public String collectShopNoticeDetail(String noticeId) throws IOException {
//        log.info("Call getPartenerNotice");
//        log.info("isValidSession: {}", isKeepSignIn(webToken));
//
//        Map<String, String> cookies = new HashMap<>();
//        cookies.put(SESSION_NAME,webToken);
//
//        Map<String, String> data = new HashMap<>();
//        data.put("list_type", orderListType.getCode());
//        if(!StringUtils.isEmpty(orderId)){
//            data.put("search_order_number", orderId);
//        }
//
//        Connection.Response getResponse = reebonzHttpClient.getOrderClaim(null, data, cookies);
//
//        return ReebonzOrderParser.parseOrderListFromWebPage(getResponse.body());
//    }

    /**
     * 판매글 내용에 이미지 목록 넣기
     */
    private String appendImageListToSaleDescription(String content, List<String> imageUrlList) {
        StringBuilder result = new StringBuilder();
        result.append(content);
        for (String imageUrl : imageUrlList) {
            result.append(String.format(
                    "<p><img src=\"%s\"></p>", imageUrl));
        }
        return result.toString();
    }

    /**
     * 상세설명에 삽입할 이미지 목록을 업로드하고 업로드된 이미지 Url 목록을 구한다
     */
    private List<String> postImageListForSaleDescription(List<ReebonzImageFileData> reebonzImageFileDataList) throws IOException {
        List<String> uploadedImageUrlList = new ArrayList<>();

        for (ReebonzImageFileData reebonzImageFileData : reebonzImageFileDataList) {
            Connection.Response imagePostResponse = reebonzHttpClient.postImageForSaleDescription(
                    modelMapper.map(reebonzImageFileData, JsoupHttpClient.FormDataForInputStream.class)
            );
            String uploadedImageUrl = reebonzSaleParser.parseUploadedImageUrl(imagePostResponse.body());

            // 리스트에 추가
            uploadedImageUrlList.add(uploadedImageUrl);
        }

        return uploadedImageUrlList;
    }

    /*
     * 판매중인 해당상품 정보 목록을 가져온다
     */
    //https://partner.reebonz.co.kr/partner/product/market_products?utf8=%E2%9C%93&search_order_created_at_from=&search_order_created_at_to=&search_name=&search_brand_id=0&search_id=9992423&search_product_meta_info_code=&per_page=20
    public String getSellingProductByProductNumber(String webToken, String productNumber) throws IOException {
        log.info("Reebonz getSellingProductByProductNumber Call");
        log.info("isValidSession: {}", isKeepSignIn(webToken));

        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME,webToken);

        Map<String, String> data = new HashMap<>();
        data.put("utf8", URLEncoder.encode("%E2%9C%93", StandardCharsets.UTF_8) );
        data.put("search_order_created_at_from", "");
        data.put("search_order_created_at_to", "");
        data.put("search_name", "");
        data.put("search_brand_id", "0"); //전체 브랜드
        data.put("search_id", productNumber);
        data.put("search_product_meta_info_code", "");
        data.put("per_page", "20");

        Connection.Response getResponse = reebonzHttpClient.getSellingProduct(null, data, cookies);

        return getResponse.body();
    }

    /**
     * 판매글에 대해 판매중지 설정/해제한다
     */
    public String postProductSaleStop(String webToken, String saleId, ReebonzSaleStatus requestStatus) throws IOException {
        //CSRF 토큰을 획득한다.
        String csrfToken = getCsrfTokenByWebToken(webToken);
        log.info("Reebonz postProductSaleStop csrfToken: {}", csrfToken);
        if(StringUtils.isEmpty(csrfToken)){
            log.error("Reebonz postProductSaleStop get csrfToken Fail!!");
            return null;
        }

        //쿠키에 csrf session 값 세팅
        Map<String, String> cookies = new HashMap<>();
        cookies.put(SESSION_NAME, webToken);

        //헤더값 세팅
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","*/*");
//        headers.put("Content-Type","multipart/form-data"); // 상품 이미지 업로드 오류 문제로 인해 주석 처리함
        headers.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
        headers.put("Referer","https://partner.reebonz.co.kr/partner/product/market_products");
        headers.put("Host","partner.reebonz.co.kr");
        headers.put("Origin","https://partner.reebonz.co.kr/partner/product/market_products");
        headers.put("X-Requested-With","XMLHttpRequest");

        // 폼 데이터 세팅
        Map<String, String> postFormData = new HashMap<>();
        postFormData.put("product_id", saleId);
        postFormData.put("available", requestStatus == ReebonzSaleStatus.SALE_STOP ? "0" : "1");
        postFormData.put("authenticity_token", csrfToken);

        Connection.Response postResponse = reebonzHttpClient.postProductSaleStop(headers, postFormData, cookies);

        return postResponse.body();
    }

    /**
     * 로그인을 위한 CSRF토큰과 쿠키 데이타
     */
    @Data
    @AllArgsConstructor
    public static class PreLoginData{
        private String authenticityToken;
        private Map<String,String> cookies;
    }

    private static class ReebonzSaleImageListSort implements Comparator<OnlineSaleImageDto> {

        @Override
        public int compare(OnlineSaleImageDto o1, OnlineSaleImageDto o2) {
            // 대표이미지 플래그가 true인 것이 순서가 먼저 위치
            if (o1.isMainFlag()) {
                return -1;
            } else if (o2.isMainFlag()) {
                return 1;
            }

            // 대표이미지 플래그가 둘다 false인 경우에는 순서값이 낮은 것이 순서가 먼저 위치
            return o1.getSequence() < o2.getSequence() ? -1
                    : o1.getSequence() > o2.getSequence() ? 1 : 0;
        }
    }

    /**
     * 상품정보고시 텍스트를 만든다
     */
    private String createProductInformationNoticeText(OnlineSaleDto onlineSaleDto) {
        StringBuilder text = new StringBuilder();

        String productionCountry = onlineSaleDto.getProductionCountry(); // 제조국
        String productionCompany = onlineSaleDto.getProductionCompany(); // 제조사
        String productionDate = onlineSaleDto.getProductionDate(); // 제조연월
        String color = onlineSaleDto.getColor();    // 색상
        String size = onlineSaleDto.getSize(); // 크기/길이/용량
        String precaution = onlineSaleDto.getPrecaution(); // 취급시 주의사항
        String csStaffName = onlineSaleDto.getCsStaffName(); // AS 책임자 이름
        String csStaffPhone = onlineSaleDto.getCsStaffPhone(); // AS 책임자 연락처
        String qualityAssuranceStandards = onlineSaleDto.getQualityAssuranceStandards(); // 품질보증기준
        String productKind = onlineSaleDto.getProductKind(); // 상품종류
        String material = onlineSaleDto.getMaterial(); // 소재/순도/재질
        String weight = onlineSaleDto.getWeight(); // 중량
        String specification = onlineSaleDto.getSpecification(); // 주요사양
        String ingredient = onlineSaleDto.getIngredient(); // 주요성분
        String mfdsCheck = onlineSaleDto.getMfdsCheck(); // 식품의약품안전처 심사필 유무
        String useByDate = onlineSaleDto.getUseByDate(); // 사용기한
        String howToUse = onlineSaleDto.getHowToUse(); // 사용방법
        boolean warrantyExistenceFlag = onlineSaleDto.isWarrantyExistenceFlag(); // 보증서 유무

        text.append("<p>");
        if (!StringUtils.isEmpty(productionCountry)) {
            text.append(String.format("- 제조국: %s<br>", productionCountry));
        }
        if (!StringUtils.isEmpty(productionCompany)) {
            text.append(String.format("- 제조사: %s<br>", productionCompany));
        }
        if (!StringUtils.isEmpty(productionDate)) {
            text.append(String.format("- 제조연월: %s<br>", productionDate));
        }
        if (!StringUtils.isEmpty(color)) {
            text.append(String.format("- 색상: %s<br>", color));
        }
        if (!StringUtils.isEmpty(size)) {
            text.append(String.format("- 크기/길이/용량: %s<br>", size));
        }
        if (!StringUtils.isEmpty(precaution)) {
            text.append(String.format("- 취급시 주의사항: %s<br>", precaution));
        }
        if (!StringUtils.isEmpty(csStaffName)) {
            text.append(String.format("- AS 책임자 이름: %s<br>", csStaffName));
        }
        if (!StringUtils.isEmpty(csStaffPhone)) {
            text.append(String.format("- AS 책임자 연락처: %s<br>", csStaffPhone));
        }
        if (!StringUtils.isEmpty(qualityAssuranceStandards)) {
            text.append(String.format("- 품질보증기준: %s<br>", qualityAssuranceStandards));
        }
        if (!StringUtils.isEmpty(productKind)) {
            text.append(String.format("- 상품종류: %s<br>", productKind));
        }
        if (!StringUtils.isEmpty(material)) {
            text.append(String.format("- 소재/순도/재질: %s<br>", material));
        }
        if (!StringUtils.isEmpty(weight)) {
            text.append(String.format("- 중량: %s<br>", weight));
        }
        if (!StringUtils.isEmpty(specification)) {
            text.append(String.format("- 주요사양: %s<br>", specification));
        }
        if (!StringUtils.isEmpty(ingredient)) {
            text.append(String.format("- 주요성분: %s<br>", ingredient));
        }
        if (!StringUtils.isEmpty(mfdsCheck)) {
            text.append(String.format("- 식품의약품안전처 심사필 유무: %s<br>", mfdsCheck));
        }
        if (!StringUtils.isEmpty(useByDate)) {
            text.append(String.format("- 사용기한: %s<br>", useByDate));
        }
        if (!StringUtils.isEmpty(howToUse)) {
            text.append(String.format("- 사용방법: %s<br>", howToUse));
        }
        text.append(String.format("- 보증서 유무: %s<br>", warrantyExistenceFlag ? "있음" : "없음"));
        text.append("</p>");

        return text.toString();
    }

}
