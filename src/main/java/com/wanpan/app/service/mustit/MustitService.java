package com.wanpan.app.service.mustit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.config.gateway.MustitClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.*;
import com.wanpan.app.dto.job.order.*;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.dto.mustit.*;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.ShopAccountToken;
import com.wanpan.app.exception.InvalidRequestException;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.mustit.constant.*;
import com.wanpan.app.service.mustit.parser.MustitHtmlParser;
import com.wanpan.app.service.mustit.parser.MustitQnAParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class MustitService implements ShopService {
    private final int ORDER_LIST_PAGE_SIZE = 10;
    private final String OLDEST_ORDER_DATE = "2010-01-01"; // 무기한 검색을 위한 가장 오래된 주문일자
    private final String COURIER_NAME_DIRECT_INPUT_CODE = "직접입력";
    private final MustitClient mustitClient;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
    private final MustitBrandService mustitBrandService;

    @Override
    public ShopAccountDto.Response checkSignIn(String loginId, String password,
                                               ShopAccountDto.Response shopAccountResponseDto)
            throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("event_no", "");
        postData.put("redirect", "");
        postData.put("id", loginId);
        postData.put("pw", password);
        postData.put("save_id", "1");

        Connection.Response mustItLoginResponse = Jsoup.connect("https://mustit.co.kr/member/login")
                .method(Connection.Method.POST)
                .data(postData)
                .followRedirects(false)
                .execute();

        log.debug(String.format("Status:%d", mustItLoginResponse.statusCode()));
        List<String> cookieValueList = mustItLoginResponse.multiHeaders().get("Set-Cookie");
        if (cookieValueList.stream().noneMatch(t -> t.contains("nowlogin=Y"))) {
            //Fail case
            shopAccountResponseDto.setSuccessFlag(false);
            Document document = mustItLoginResponse.parse();
            shopAccountResponseDto.setMessage(document.select("script").html().split("'")[1]);
        } else {
            shopAccountResponseDto.setSuccessFlag(true);
            for(String cookie : cookieValueList){
                log.info("response cookie:{}", cookie);
            }
        }

        return shopAccountResponseDto;
    }

    @Override
    public boolean isKeepSignIn(String token, String accountId, ShopAccountToken.Type tokenType) {
        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", token);
            Connection.Response response = mustitClient.getMyPage(headers);

            if (MustitHtmlParser.isKeepSignIn(response.body(), accountId)) {
                return true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return false;
    }

    @Override
    public String getToken(String accountId, String password, ShopAccountToken.Type tokenType) {
        //signin에 필요한 form data를 구성한다.
//        SignIn signIn = new SignIn(accountId, password);
        MustitSignInRequest mustitSignInRequest = new MustitSignInRequest(accountId, password);
        Map<String, String> signInFormMap = objectMapper.convertValue(mustitSignInRequest, Map.class);
        try {
            Connection.Response response = mustitClient.login(signInFormMap,null);
            log.info("getToken response statusCode: {}",response.statusCode());
            if(response.statusCode() == 200 || response.statusCode() == 302){
                List<String> cookieValueList = response.multiHeaders().get("Set-Cookie");

                Document document = response.parse();
                if(cookieValueList.stream().noneMatch(t->t.contains("nowlogin=Y"))){
                    //Fail case
                    log.error(document.select("script").html().split("'")[1]);
                    return null;
                }else{
                    //Success
                    log.info("Login Success!!");
                    for(String cookie : cookieValueList){
                        log.info("response cookie:{}", cookie);
                    }
                    //쿠키 값중에 여러가지가 다 들어가야 하기 때문에 필요한 여러 값들만 추출해 낸다
                    return getRequiredCookie(response);
                }
            }
        } catch (IOException e) {
            log.error("MUSTIT getToken Fail, IOException", e);
        } catch (NullPointerException e) {
            log.error("unexpected null data is arrived from MustIt", e);
        } catch (Exception e) {
            log.error("unexpected exception occurred during crawl and save MustIt data.", e);
        }

        return null;
    }

    @Override
    public List<BrandDto> getBrandList(final String token) {
        return mustitBrandService.getBrandListBySearchName(token, "");
    }

    @Override
    public List<CategoryDto> getCategoryList(String token) {
        return null;
    }

    @Async
    @Override
    public ListenableFuture<RegisterDto.Response> postSaleToShop(String token, Job job, OnlineSaleDto onlineSaleDto) {
        log.info("Mustit postSaleToShop JOB START => jobId={}", job.getId());

        Connection.Response response = null;
        String responseBody = null;
        String saleId = null;
        boolean successFlag;
        String message = null;

        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", token);

        try {
            List<Pair<String, String>> saleRegistrationData = mapToMustitSaleFormData(onlineSaleDto, null);
            log.info("");
            log.info("Before Mustit postSaleToShop Form =>\n{}", saleRegistrationData);
            response = mustitClient.registerSale(headers, saleRegistrationData);
            responseBody = response.body();
            log.info("After Mustit postSaleToShop Response Body =>\n{}", responseBody);
            saleId = MustitHtmlParser.getSaleId(responseBody);
            if (saleId == null) {
                throw new Exception(responseBody);
            }
            successFlag = true;
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;

            if (response != null) {
                message = MustitHtmlParser.getAlertMessage(responseBody);
            }
        }

        log.info("Mustit postSaleToShop JOB END => jobId={}", job.getId());

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                saleId,
                ShopSaleJobDto.SaleStatus.ON_SALE,
                job.getId(),
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message
        ));
    }

    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleToShop(String token, long jobId, OnlineSaleDto onlineSaleDto) {
        log.info("Mustit updateSaleToShop JOB START => jobId={}", jobId);

        ShopSaleJobDto.Request.PostJob shopSaleDto = onlineSaleDto.getShopSale();
        String saleId = shopSaleDto.getPostId();
        Connection.Response response = null;
        String responseBody = null;
        boolean successFlag;
        String message = null;
        ShopSaleJobDto.SaleStatus currentStatus = null;

        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", token);

        try {
            MustitSaleStatus collectedMustitSaleStatus = getMustitSaleStatus(headers, saleId);

            if (collectedMustitSaleStatus == MustitSaleStatus.NOT_FOUND_SALE) {
                // 현재 쇼핑몰 판매상태가 삭제된 상태이거나 다른 사용자의 판매글일 수 있는 경우
                log.info("Mustit updateSaleToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        saleId,
                        ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "해당 판매글은 현재 머스트잇에서 삭제된 상태입니다."
                ));
            } else if (collectedMustitSaleStatus == MustitSaleStatus.SALE_STOP) {
                // 현재 쇼핑몰 판매상태가 판매중지 상태인 경우

                return new AsyncResult<>(new RegisterDto.Response(
                        shopSaleDto.getId(),
                        saleId,
                        ShopSaleJobDto.SaleStatus.SALE_STOP,
                        jobId,
                        shopSaleDto.getId(),
                        false,
                        "해당 판매글은 현재 머스트잇에서 판매중지된 상태입니다."
                ));
            } else {
                // 현재 쇼핑몰 판매상태가 판매수정이 가능한 상태인 경우

                Connection.Response getSaleModificationPageResponse = mustitClient.getSaleModificationPage(headers, onlineSaleDto.getShopSale().getPostId());
                MustitSale oldMustitSale = MustitHtmlParser.getSale(getSaleModificationPageResponse.body());
                oldMustitSale.setSaleId(saleId);
                List<Pair<String, String>> saleUpdateData = mapToMustitSaleFormData(onlineSaleDto, oldMustitSale);
                response = mustitClient.updateSale(headers, saleUpdateData, saleId);
                log.info("Before Mustit updateSaleToShop Form =>\n{}", responseBody);
                responseBody = response.body();
                log.info("After Mustit updateSaleToShop Response Body =>\n{}", responseBody);
                if (MustitHtmlParser.getSaleId(responseBody) == null) {
                    throw new Exception(responseBody);
                }
                successFlag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;

            if (response != null) {
                message = MustitHtmlParser.getAlertMessage(responseBody);
            }
        }

        // 판매글 업데이트 성공이든 실패든 최종 쇼핑몰 판매글 상태 수집
        try {
            MustitSaleStatus mustitSaleStatus = getMustitSaleStatus(headers, saleId);
            currentStatus = ShopSaleJobDto.SaleStatus.valueOf(mustitSaleStatus.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Mustit updateSaleToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                shopSaleDto.getId(),
                saleId,
                currentStatus,
                jobId,
                shopSaleDto.getId(),
                successFlag,
                message
        ));
    }

    @Async
    @Override
    public ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String token, long jobId, ShopQnaJobDto.QuestionStatus questionStatus, ShopAccountDto.Request request) {
        try {
            log.info("Call collectQnAFromShop");
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", token);
            //검색 파라미터 설정(파라미터에 따라서 수집하던거를 둘다 수집으로 변경)
            Map<String, String> data = new HashMap<>();
            data.put("search_date", "12m");//1m,3m,6m,12m
            //        data.put("fromDt", "2020-06-03");//2020-06-03
            //        data.put("toDt", "2020-07-03");//2020-07-03
            //답변완료수집
            data.put("searchAnswer", "Y");
            Connection.Response  completeResponse = mustitClient.getQna(headers, data, null);
            List<ShopQnaDto.Request.CollectCallback> shopQnAList = MustitQnAParser.parseQna(completeResponse.body(), request);
            //답변대기수집
            data.put("searchAnswer", "N");
            Connection.Response  readyResponse = mustitClient.getQna(headers, data, null);
            shopQnAList.addAll(MustitQnAParser.parseQna(readyResponse.body(), request));

            //최종 response 구성
            ShopQnaJobDto.Request.CollectCallback collectShopQnAListCallback = new ShopQnaJobDto.Request.CollectCallback();
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setSuccessFlag(true);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setMessage("수집성공");
            collectShopQnAListCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
            collectShopQnAListCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(collectShopQnAListCallback);
        }catch(Exception e){
            log.error("Failed",e);
            return null;
        }
    }

    @Async
    @Override
    public ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String token, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request) {
        return null;
    }

    /**
     *
     * @param token 로그인 세션 코튼
     * @param jobId 요청 job id
     * @param postJobDto 요청 json
     * @return ShopQnaJobDto.Request.PostCallback
     */
    @Override
    public ListenableFuture<ShopQnaJobDto.Request.PostCallback> postAnswerForQnaToShop(String token, long jobId, ShopQnaJobDto.Request.PostJob postJobDto) {
        log.info("Mustit postAnswerForQnaToShop Call");
        boolean successFlag = false;
        String resultMessage = null;
        try {
            log.info("Call collectQna");
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", token);
            //검색 파라미터 설정
            Map<String, String> data = new HashMap<>();
            data.put("groups", postJobDto.getShopQna().getQuestionId());//문의글 번호
            data.put("seller_id", postJobDto.getShopQna().getShopQnaConversation().getWriterId());//"판매자(답변자) ID
            data.put("bbs_review", postJobDto.getShopQna().getShopQnaConversation().getContent());//답변내용
            data.put("userfile", "");//빈값으로 고정해서 사용. 파일첨부기능 지원시 binary 사용.
            data.put("post_lock", "1");//필드값 의미 모름. 1로 고정해서 사용

            Connection.Response  response = mustitClient.postAnswerForQna(headers, data, null);
//            log.info("response.body() : {}", response.body());

            resultMessage = PatternExtractor.MUSTIT_POST_ANSWER_RESULT.extract(response.body(),1);
//            log.info("resultMessage : {}", resultMessage);
            successFlag = true;

            //답변 등록시에 해당 Question에 해당하는 목록을 보냄으로써 실제 등록된걸로 업데이트 처리한다.
            //검색 파라미터 설정
            Map<String, String> getParamData = new HashMap<>();
            getParamData.put("searchAnswer", "Y");//"Y,N"
            getParamData.put("search_date", "12m");//1m,3m,6m,12m
            getParamData.put("fromDt", "");//"Y,N"
            getParamData.put("toDt", "");//"Y,N"
            getParamData.put("keyword", postJobDto.getShopQna().getQuestionTitle());
            getParamData.put("search_key", "bbs_title");//"Y,N"
//            log.info("Question title: {}", postJobDto.getShopQna().getQuestionTitle());
            Connection.Response  collectResponse = mustitClient.getQna(headers, getParamData, null);
//            log.info("collectResponse : {}",collectResponse.body());
            List<ShopQnaDto.Request.PostCallback> shopQnAList = MustitQnAParser.parseQnaByQuestionId(
                    collectResponse.body(), postJobDto.getShopAccount(), postJobDto.getShopQna().getQuestionId());
//            shopQnAList.sort((p1, p2) -> p2.getQuestionId().compareTo(p1.getQuestionId()));
            //최종 response 구성
            ShopQnaJobDto.Request.PostCallback postAnswerCallback = new ShopQnaJobDto.Request.PostCallback();
            postAnswerCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            postAnswerCallback.getJobTaskResponseBaseDto().setRequestId(postJobDto.getShopQna().getShopQnaConversation().getRequestId());
            postAnswerCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
            postAnswerCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
            postAnswerCallback.setShopAccount(modelMapper.map(postJobDto.getShopAccount(), ShopAccountDto.Response.class));
            postAnswerCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(postAnswerCallback);
        }catch(Exception e){
            log.error("Failed",e);
            return null;
        }
    }

    @Override
    public ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob) {
        log.info("Mustit deleteShopSale JOB START => jobId={}", jobId);

        String saleId = deleteSaleJob.getPostId();
        boolean successFlag = false;
        String resultMessage;
        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", token);
        try {
            // 머스트잇은 판매글 삭제에 대한 아무런 제약이 없어, 다른 판매자의 판매글도 삭제할 수도 있으므로 주의해야 한다.

            MustitSaleStatus collectedMustitSaleStatus = getMustitSaleStatus(headers, saleId);
            if (collectedMustitSaleStatus == MustitSaleStatus.NOT_FOUND_SALE) {
                // 현재 쇼핑몰 판매상태가 삭제된 상태이거나 다른 사용자의 판매글일 수 있는 경우
                log.info("Mustit deleteShopSale JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        deleteSaleJob.getId(), //shopSaleId
                        saleId,
                        ShopSaleJobDto.SaleStatus.DELETE,
                        jobId,
                        deleteSaleJob.getId(),
                        true,
                        null
                ));
            } else {
                // 현재 쇼핑몰 판매상태를 수집하는 데에 성공한 상황이고 자신의 판매글이 확실한 상황이므로 판매글 삭제 요청 시도
                Connection.Response response = mustitClient.deleteSale(saleId);
                if (isSaleDeleted(response.body())) {
                    successFlag = true;
                }
                resultMessage = MustitHtmlParser.getAlertMessage(response.body());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            successFlag = false;
            resultMessage = "해당 머스트잇 판매글에 대한 삭제 요청이 실패하였습니다.";
        }

        return new AsyncResult<>(new RegisterDto.Response(
                deleteSaleJob.getId(), //shopSaleId
                saleId,
                ShopSaleJobDto.SaleStatus.DELETE,
                jobId,
                deleteSaleJob.getId(),
                successFlag,
                resultMessage
        ));
    }

//    private boolean deleteProductFromShop(String token, String saleId) {
//        // TODO: MUSTIT 판매 삭제 요청 시에 토큰 정보가 필요 없어서 다른 MUSTIT 사용자의 판매 데이터를 삭제하지 않도록 주의해야 한다.
//        // TODO: 구매자가 있는 판매글을 삭제할 경우에 대한 확인도 필요하다.
//
//        try {
//            Connection.Response response = mustitClient.deleteSale(saleId);
//
//            return MustitHtmlParser.isSaleDeleted(response.body());
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.debug("[deleteProductFromShop()] 머스트잇 판매 삭제 실패");
//        }
//
//        return false;
//    }

    @Override
    public ListenableFuture<OrderJobDto.Request.PostConversationCallback> postConversationMessageForOrderToShop(String token, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob) {
        OrderJobDto.Request.PostConversationCallback postConversationCallbackDto = new OrderJobDto.Request.PostConversationCallback();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", token);
        Map<String, String> formData = new HashMap<>();
        String shopOrderId = postConversationJob.getShopOrderId();
        String shopOrderUniqueId = postConversationJob.getShopUniqueOrderId();
        formData.put("number", shopOrderUniqueId);
        formData.put("comment", postConversationJob.getOrderConversationMessage());
        boolean successFlag;
        String resultMessage;

        try {
            Connection.Response orderConversationMessagePostResponse = mustitClient.sendOrderConversationMessage(headers, formData, shopOrderUniqueId);
            if (orderConversationMessagePostResponse.statusCode() == 302) {
                successFlag = true;
                resultMessage = "주문대화 메세지 전송 성공";
            } else {
                successFlag = false;
                resultMessage = "주문대화 메세지 전송 실패";
            }
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;
            resultMessage = "주문대화 메세지 전송 실패";
        }

        try {
            // 주문대화 수집
            OrderBaseConversationDto orderBaseConversationDto = getOrderBaseConversation(headers, shopOrderId, shopOrderUniqueId);
            if (orderBaseConversationDto != null) {
                postConversationCallbackDto.getOrderBaseConversationList().add(orderBaseConversationDto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        postConversationCallbackDto.getJobTaskResponseBaseDto().setJobId(jobId);
        postConversationCallbackDto.getJobTaskResponseBaseDto().setRequestId(postConversationJob.getShopAccount().getRequestId());
        postConversationCallbackDto.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        postConversationCallbackDto.getJobTaskResponseBaseDto().setMessage(resultMessage);
        postConversationCallbackDto.setShopAccount(modelMapper.map(postConversationJob.getShopAccount(), ShopAccountDto.Response.class));

        return new AsyncResult<>(postConversationCallbackDto);
    }

    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleStatusToShop(String token, long jobId, ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob) {
        log.info("Mustit updateSaleStatusToShop Call => jobId={}", jobId);

        ShopSaleJobDto.SaleStatus requestSaleStatus = updateSaleStatusJob.getRequestSaleStatus();
        String saleId = updateSaleStatusJob.getPostId();
        boolean successFlag = false;
        String message = null;
        ShopSaleJobDto.SaleStatus currentSaleStatus = null;
        Connection.Response response;

        HashMap<String, String> headers = new HashMap<>();
        headers.put("cookie", token);

        try {
            MustitSaleStatus collectedMustitSaleStatus = getMustitSaleStatus(headers, saleId);
            if (collectedMustitSaleStatus == MustitSaleStatus.NOT_FOUND_SALE) {
                // 현재 쇼핑몰 판매상태가 삭제된 상태이거나 다른 사용자의 판매글일 수 있는 경우
                log.info("Mustit updateSaleStatusToShop JOB END => jobId={}", jobId);
                return new AsyncResult<>(new RegisterDto.Response(
                        updateSaleStatusJob.getId(),
                        saleId,
                        ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE,
                        jobId,
                        updateSaleStatusJob.getId(),
                        false,
                        "해당 판매글은 현재 머스트잇에서 삭제된 상태입니다."
                ));
            } else {
                switch (requestSaleStatus) {
                    case SALE_STOP: // "판매중지" 설정
                        response = mustitClient.updateSaleStatusToSaleStop(headers, saleId);
                        break;
                    case ON_SALE: // "판매중지" 해제
                        response = mustitClient.updateSaleStatusToSaleStopCancel(headers, saleId);
                        break;
                    default:
                        throw new InvalidRequestException("불가능한 판매글상태변경요청값: " + requestSaleStatus.name());
                }

                if (response != null) {
                    message = MustitHtmlParser.getAlertMessage(response.body());
                    if (message != null) {
                        successFlag = isSaleStatusUpdated(message);
                        if (successFlag) {
                            currentSaleStatus = requestSaleStatus;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Mustit updateSaleStatusToShop Error =>", e);
            successFlag = false;
        }

        // 판매상태 업데이트 성공이든 실패든 최종 쇼핑몰 판매글 상태 수집
        try {
            MustitSaleStatus mustitSaleStatus = getMustitSaleStatus(headers, saleId);
            currentSaleStatus = ShopSaleJobDto.SaleStatus.valueOf(mustitSaleStatus.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Mustit updateSaleStatusToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                updateSaleStatusJob.getId(),
                saleId,
                currentSaleStatus,
                jobId,
                updateSaleStatusJob.getId(),
                successFlag,
                message
        ));
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.CollectCallback> collectOrderFromShop(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request) {
        log.info("[collectOrderFromShop()] START => jobId={}", jobId);

        OrderJobDto.Request.CollectCallback orderJobDto = new OrderJobDto.Request.CollectCallback();
        boolean successFlag;
        String resultMessage;
        String orderSearchStartDate = request.getLatestCollectOrderAt() != null ? request.getLatestCollectOrderAt() : OLDEST_ORDER_DATE; // 주문검색시작일자
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 주문수집 실행일
        String oldestOrderDateOfOngoingOrders = null;
        List<OrderDto.Request.CollectCallback> ongoingOrderList = new ArrayList<>(); // "진행주문" 리스트
        List<OrderDto.Request.CollectCallback> completeOrderList = new ArrayList<>(); // "완료주문" 리스트
        boolean calculationRequestOrderExistenceFlag = false; // "정산예정 주문" 존재 여부
        List<LocalDateTime> moniteringTargetOrderDateList = new ArrayList<>(); // 모니터링 대상 주문들의 결제일자 리스트

        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("cookie", token);

            // 1. 진행주문관리>발송요청 목록 (무기한 검색)
            Connection.Response deliveryRequestOrderResponse = mustitClient.getDeliveryRequestOrderList(headers);
            Elements deliveryRequestOrderWorkbook = deliveryRequestOrderResponse.parse().body().getElementsByTag("workbook");
            Elements deliveryRequestOrderRows = deliveryRequestOrderWorkbook.select("row");
            deliveryRequestOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(deliveryRequestOrderRows)) {
                for (Element row : deliveryRequestOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    orderDto.setPersonalClearanceCode(data.get(21).text()); // 개인통관부호

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 2. 진행주문관리>발송준비중 목록 (무기한 검색)
            Connection.Response deliveryReadyOrderResponse = mustitClient.getDeliveryReadyOrderList(headers);
            Elements deliveryReadyOrderWorkbook = deliveryReadyOrderResponse.parse().body().getElementsByTag("workbook");
            Elements deliveryReadyOrderRows = deliveryReadyOrderWorkbook.select("row");
            deliveryReadyOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(deliveryReadyOrderRows)) {
                for (Element row : deliveryReadyOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    orderDto.setPersonalClearanceCode(data.get(21).text()); // 개인통관부호

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 3. 진행주문관리>배송중 목록 (무기한 검색)
            Connection.Response deliveringOrderResponse = mustitClient.getDeliveringOrderList(headers);
            Elements deliveringOrderWorkbook = deliveringOrderResponse.parse().body().getElementsByTag("workbook");
            Elements deliveringOrderRows = deliveringOrderWorkbook.select("row");
            deliveringOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(deliveringOrderRows)) {
                for (Element row : deliveringOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    String courierName = data.get(21).text();
                    orderDto.setCourierCode(Objects.requireNonNullElse(MustitCourier.getByName(courierName), MustitCourier.CUSTOM_COURIER_NAME).getCode()); // 택배사 코드
                    orderDto.setCourierName(courierName); // 택배사 이름
                    orderDto.setTrackingNumber(data.get(22).text()); // 송장번호
                    orderDto.setPersonalClearanceCode(data.get(25).text()); // 개인통관부호

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }


            // 4. 진행주문관리>배송완료 목록 (무기한 검색)
            Connection.Response deliveryCompletionOrderResponse = mustitClient.getDeliveryCompletionOrderList(headers);
            Elements deliveryCompletionOrderWorkbook = deliveryCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements deliveryCompletionOrderRows = deliveryCompletionOrderWorkbook.select("row");
            deliveryCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(deliveryCompletionOrderRows)) {
                for (Element row : deliveryCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    String courierName = data.get(21).text();
                    orderDto.setCourierCode(Objects.requireNonNullElse(MustitCourier.getByName(courierName), MustitCourier.CUSTOM_COURIER_NAME).getCode()); // 택배사 코드
                    orderDto.setCourierName(courierName); // 택배사 이름
                    orderDto.setTrackingNumber(data.get(22).text()); // 송장번호
                    orderDto.setPersonalClearanceCode(data.get(25).text()); // 개인통관부호

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 5. 진행주문관리>구매취소요청 목록 (무기한 검색)
            Connection.Response buyCancelRequestOrderResponse = mustitClient.getBuyCancelRequestOrderList(headers);
            Elements buyCancelRequestOrderWorkbook = buyCancelRequestOrderResponse.parse().body().getElementsByTag("workbook");
            Elements buyCancelRequestOrderRows = buyCancelRequestOrderWorkbook.select("row");
            buyCancelRequestOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(buyCancelRequestOrderRows)) {
                for (Element row : buyCancelRequestOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    orderDto.setPurchaseCancellationReason(data.get(21).text()); // 구매취소 사유

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 6. 진행주문관리>교환반품요청 목록 (무기한 검색)
            Connection.Response exchangeReturnRequestOrderResponse = mustitClient.getExchangeReturnRequestOrderList(headers);
            Elements exchangeReturnRequestOrderWorkbook = exchangeReturnRequestOrderResponse.parse().body().getElementsByTag("workbook");
            Elements exchangeReturnRequestOrderRows = exchangeReturnRequestOrderWorkbook.select("row");
            exchangeReturnRequestOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(exchangeReturnRequestOrderRows)) {
                for (Element row : exchangeReturnRequestOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    switch (orderDto.getStatus()) {
                        case EXCHANGE_REQUEST: // 교환요청인 경우
                            orderDto.setExchangeReason(data.get(21).text()); // 교환 사유
                            break;
                        case RETURN_REQUEST: // 반품요청인 경우
                            orderDto.setReturnReason(data.get(21).text()); // 반품 사유
                            break;
                        default:
                            break;
                    }

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 7. 진행주문관리>반품성사 목록 (무기한 검색)
            Connection.Response returnCompletionOrderResponse = mustitClient.getReturnCompletionOrderList(headers);
            Elements returnCompletionOrderWorkbook = returnCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements returnCompletionOrderRows = returnCompletionOrderWorkbook.select("row");
            returnCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(returnCompletionOrderRows)) {
                for (Element row : returnCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    orderDto.setReturnReason(data.get(21).text()); // 반품 사유 세팅

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 8. 진행주문관리>판매취소 목록 (무기한 검색)
            Connection.Response saleCancelOrderResponse = mustitClient.getSaleCancelOrderList(headers);
            Elements saleCancelOrderWorkbook = saleCancelOrderResponse.parse().body().getElementsByTag("workbook");
            Elements saleCancelOrderRows = saleCancelOrderWorkbook.select("row");
            saleCancelOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(saleCancelOrderRows)) {
                for (Element row : saleCancelOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.ONGOING, orderDto, data);
                    orderDto.setSaleCancellationReason(data.get(21).text()); // 판매취소 사유 세팅

                    // "진행주문" 리스트에 추가
                    ongoingOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 9. 완료주문관리>정산예정 목록 (무기한 검색)
            Connection.Response calculationScheduleOrderResponse = mustitClient.getCalculationScheduleOrderList(headers, OLDEST_ORDER_DATE, today);
            Elements calculationScheduleOrderWorkbook = calculationScheduleOrderResponse.parse().body().getElementsByTag("workbook");
            Elements calculationScheduleOrderRows = calculationScheduleOrderWorkbook.select("row");
            calculationScheduleOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(calculationScheduleOrderRows)) {
                calculationRequestOrderExistenceFlag = true;
                for (Element row : calculationScheduleOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.COMPLETE, orderDto, data);
                    orderDto.setCalculateAmount(data.get(18).text()); // 정산예정금액

                    // "완료주문" 리스트에 추가
                    completeOrderList.add(orderDto);

                    // 모니터링 대상 주문들의 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }
            }

            // 10. 완료주문관리>정산완료 목록
            Connection.Response calculationCompletionOrderResponse = mustitClient.getCalculationCompletionOrderList(headers, orderSearchStartDate, today);
            Elements calculationCompletionOrderWorkbook = calculationCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements calculationCompletionOrderRows = calculationCompletionOrderWorkbook.select("row");
            calculationCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(calculationCompletionOrderRows)) {
                for (Element row : calculationCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.COMPLETE, orderDto, data);
                    orderDto.setCalculateAmount(data.get(18).text()); // 정산금액
                    orderDto.setCalculateDate(data.get(19).text()); // 정산완료일

                    // "완료주문" 리스트에 추가
                    completeOrderList.add(orderDto);
                }
            }

            // 11. 완료주문관리>구매취소완료 목록
            Connection.Response buyCancelCompletionOrderResponse = mustitClient.getBuyCancelCompletionOrderList(headers, orderSearchStartDate, today);
            Elements buyCancelCompletionOrderWorkbook = buyCancelCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements buyCancelCompletionOrderRows = buyCancelCompletionOrderWorkbook.select("row");
            buyCancelCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(buyCancelCompletionOrderRows)) {
                for (Element row : buyCancelCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.COMPLETE, orderDto, data);
                    orderDto.setPurchaseCancellationReason(data.get(15).text()); // 구매취소 사유 세팅

                    // "완료주문" 리스트에 추가
                    completeOrderList.add(orderDto);
                }
            }

            // 12. 완료주문관리>반품환불완료 목록
            Connection.Response returnRefundCompletionOrderResponse = mustitClient.getReturnRefundCompletionOrderList(headers, orderSearchStartDate, today);
            Elements returnRefundCompletionOrderWorkbook = returnRefundCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements returnRefundCompletionOrderRows = returnRefundCompletionOrderWorkbook.select("row");
            returnRefundCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(returnRefundCompletionOrderRows)) {
                for (Element row : returnRefundCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.COMPLETE, orderDto, data);
                    orderDto.setReturnReason(data.get(15).text()); // 반품 사유 세팅

                    // "완료주문" 리스트에 추가
                    completeOrderList.add(orderDto);
                }
            }

            // 13. 완료주문관리>판매취소완료 목록
            Connection.Response saleCancelCompletionOrderResponse = mustitClient.getSaleCancelCompletionOrderList(headers, orderSearchStartDate, today);
            Elements saleCancelCompletionOrderWorkbook = saleCancelCompletionOrderResponse.parse().body().getElementsByTag("workbook");
            Elements saleCancelCompletionOrderRows = saleCancelCompletionOrderWorkbook.select("row");
            saleCancelCompletionOrderRows.remove(0); // 컬럼명이 있는 row는 제거
            if (!CollectionUtils.isEmpty(saleCancelCompletionOrderRows)) {
                for (Element row : saleCancelCompletionOrderRows) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    Elements data = row.getElementsByTag("data");
                    setOrderCommonData(MustitOrderStatusGroupType.COMPLETE, orderDto, data);
                    orderDto.setSaleCancellationReason(data.get(15).text()); // 판매취소 사유

                    // "완료주문" 리스트에 추가
                    completeOrderList.add(orderDto);
                }
            }

            // 수집한 "진행주문" 데이터 가공 작업
            if (!ongoingOrderList.isEmpty()) {
                /////// "진행주문" 관련 (주문번호, 주문상태, 배송정보, 교환배송정보, 반품배송정보) 교체
                replaceOrderInfo(headers, ongoingOrderList, MustitOrderStatusGroupType.ONGOING, null);
                // "진행주문" 관련 (주문자정보) 교체
                replaceOrderInfo(headers, ongoingOrderList, MustitOrderStatusGroupType.ALL, null);

                /////// 주문대화 수집 및 "진행주문" 결제일자 리스트에 추가
                Set<String> shopOrderIdSet = new HashSet<>();
                for (OrderDto.Request.CollectCallback orderDto: ongoingOrderList) {
                    String shopOrderId = orderDto.getOrderId();
                    String shopOrderUniqueId = orderDto.getOrderUniqueId();

                    // 수집한 적 없는 주문대화 페이지만 수집
                    if (!shopOrderIdSet.contains(shopOrderId)) {
                        OrderBaseConversationDto orderBaseConversation = getOrderBaseConversation(headers, shopOrderId, shopOrderUniqueId);
                        if (orderBaseConversation != null) {
                            orderDto.getOrderBaseConversationList().add(orderBaseConversation);
                        }
                        shopOrderIdSet.add(shopOrderId);
                    }

                    // "진행주문" 결제일자 리스트에 추가
                    moniteringTargetOrderDateList.add(orderDto.getOrderDate());
                }

                // 콜백 요청용 주문 리스트에 "진행주문" 리스트 추가
                orderJobDto.getOrderList().addAll(ongoingOrderList);
            }


            /////// 수집한 "완료주문" 데이터 가공 작업
            if (!completeOrderList.isEmpty()) {
                // "완료주문" 관련 (주문번호) 교체
                replaceOrderInfo(headers, completeOrderList, MustitOrderStatusGroupType.COMPLETE, orderSearchStartDate);
                // "완료주문" 관련 (주문자정보) 교체
                replaceOrderInfo(headers, completeOrderList, MustitOrderStatusGroupType.ALL, null);

                if (calculationRequestOrderExistenceFlag) {
                    /////// "정산예정 주문" 관련 (주문상태) 교체
                    replaceOrderInfo(headers, completeOrderList, MustitOrderStatusGroupType.CALCULATION_SCHEDULE, null);
                }

                // 콜백 요청용 주문 리스트에 "완료주문" 리스트 추가
                orderJobDto.getOrderList().addAll(completeOrderList);
            }

            successFlag = true;
            resultMessage = "주문수집 성공";

        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;
            resultMessage = "주문수집 실패";
        }

        orderJobDto.getJobTaskResponseBaseDto().setJobId(jobId);
        orderJobDto.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        orderJobDto.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        orderJobDto.getJobTaskResponseBaseDto().setMessage(resultMessage);
        orderJobDto.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));

        /////// 다음번 수집시작날짜 세팅
        if (successFlag) {
            if (!moniteringTargetOrderDateList.isEmpty()) {
                // 모니터링 대상 주문 기준으로 가장 오래된 주문일자로 세팅
                Collections.sort(moniteringTargetOrderDateList); // 오름차순 정렬
                oldestOrderDateOfOngoingOrders = moniteringTargetOrderDateList.get(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                orderJobDto.getShopAccount().setLatestCollectOrderAt(oldestOrderDateOfOngoingOrders);
            } else {
                // 오늘 날짜로 세팅
                orderJobDto.getShopAccount().setLatestCollectOrderAt(today);
            }
        } else {
            // 수집 시도했던 검색시작일자로 세팅
            orderJobDto.getShopAccount().setLatestCollectOrderAt(orderSearchStartDate);
        }

        return new AsyncResult<>(orderJobDto);
    }

    /**
     * 페이지 파싱을 통해 교체해야 하는 주문정보를 수집하고 교체한다
     */
    private void replaceOrderInfo(Map<String, String> headers, List<OrderDto.Request.CollectCallback> orderList, MustitOrderStatusGroupType mustitOrderStatusGroupType, String completeOrderSearchStartDate) throws IOException {
        MustitOrderInfoForReplace mustitOrderInfoForReplace = new MustitOrderInfoForReplace();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date()); // 주문수집 실행일
        if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.ONGOING) { // "진행주문"
            Connection.Response ongoingOrderPageResponse = mustitClient.getOngoingOrderListPage(headers, 0);
            int targetOngoingOrderCount = MustitHtmlParser.getOngoingOrderCount(ongoingOrderPageResponse.body());
            if (targetOngoingOrderCount != 0) {
                int pageCount = getPageCount(targetOngoingOrderCount);
                for (int i = 0; i < pageCount; i++) {
                    int offset = ORDER_LIST_PAGE_SIZE * i;
                    Connection.Response orderPageResponse2 = mustitClient.getOngoingOrderListPage(headers, offset);
                    // 교체 대상 정보 수집
                    MustitHtmlParser.collectOrderInfoForReplaceFromOrderListPage(orderPageResponse2.body(), mustitOrderInfoForReplace, MustitOrderStatusGroupType.ONGOING);
                }

                // 교체 대상 정보 세팅
                Map<String, String> ongoingOrderIdMap = mustitOrderInfoForReplace.getOrderIdMap();
                Map<String, String> ongoingOrderStatusMap = mustitOrderInfoForReplace.getOrderStatusMap();
                Map<String, MustitDeliveryInfo> deliveryInfoMap = mustitOrderInfoForReplace.getDeliveryInfoMap();
                Map<String, MustitDeliveryInfo> exchangeDeliveryInfoMap = mustitOrderInfoForReplace.getExchangeDeliveryInfoMap();
                Map<String, MustitDeliveryInfo> returnDeliveryInfoMap = mustitOrderInfoForReplace.getReturnDeliveryInfoMap();
                for (OrderDto.Request.CollectCallback orderDto: orderList) {
                    String orderId = orderDto.getOrderId();
                    String orderUniqueId = orderDto.getOrderUniqueId();

                    // 주문번호 교체
                    if (ongoingOrderIdMap.containsKey(orderId)) {
                        orderDto.setOrderId(ongoingOrderIdMap.get(orderId)); // 주문번호
                    }

                    // 주문상태 교체
                    if (ongoingOrderStatusMap.containsKey(orderUniqueId)) {
                        orderDto.setStatus(OrderDto.OrderStatus.getByCode(ongoingOrderStatusMap.get(orderUniqueId))); // 주문상태
                    }

                    // 배송정보 교체
                    if (deliveryInfoMap.containsKey(orderUniqueId)) {
                        Connection.Response orderDeliveryInfoPageResponse = mustitClient.getOrderDeliveryInfoPage(headers, orderUniqueId);
                        MustitDeliveryInfo mustitDeliveryInfo = MustitHtmlParser.getDeliveryInfo(orderDeliveryInfoPageResponse.body());
                        String courierName = mustitDeliveryInfo.getCourierName();
                        MustitCourier mustitCourier = MustitCourier.getByName(courierName);
                        if (mustitCourier != null) {
                            orderDto.setCourierCode(mustitCourier.getCode()); // 택배사 코드
                            orderDto.setCourierName(courierName); // 택배사 이름
                        } else {
                            orderDto.setCourierCode(MustitCourier.CUSTOM_COURIER_NAME.getCode()); // 택배사 코드
                            orderDto.setCourierCustomName(courierName); // 직접입력한 택배사 이름
                        }
                        orderDto.setTrackingNumber(mustitDeliveryInfo.getTrackingNumber()); // 송장번호
                        orderDto.setDeliveryType(mustitDeliveryInfo.getDeliveryType());
                    }

                    // 교환배송정보 세팅
                    if (exchangeDeliveryInfoMap.containsKey(orderUniqueId)) {
                        Connection.Response orderExchangeDeliveryInfoPageResponse = mustitClient.getExchangeOrderDeliveryInfoPage(headers, orderUniqueId);
                        MustitDeliveryInfo mustitExchangeDeliveryInfo = MustitHtmlParser.getDeliveryInfo(orderExchangeDeliveryInfoPageResponse.body());
                        String exchangeCourierName = mustitExchangeDeliveryInfo.getCourierName();
                        MustitCourier mustitExchangeCourier = MustitCourier.getByName(exchangeCourierName);
                        if (mustitExchangeCourier != null) {
                            orderDto.setExchangeCourierCode(mustitExchangeCourier.getCode()); // 교환 택배사 코드
                            orderDto.setExchangeCourierName(exchangeCourierName); // 교환 택배사 이름
                        } else {
                            orderDto.setExchangeCourierCode(MustitCourier.CUSTOM_COURIER_NAME.getCode()); // 교환 택배사 코드
                            orderDto.setExchangeCourierCustomName(exchangeCourierName); // 직접입력한 교환 택배사 이름
                        }
                        orderDto.setExchangeTrackingNumber(mustitExchangeDeliveryInfo.getTrackingNumber()); // 교환 송장번호
                    }

                    // 반품배송정보 세팅
                    if (returnDeliveryInfoMap.containsKey(orderUniqueId)) {
                        String returnCourierName = returnDeliveryInfoMap.get(orderUniqueId).getCourierName();
                        MustitCourier mustitReturnCourier = MustitCourier.getByName(returnCourierName);
                        if (mustitReturnCourier != null) {
                            orderDto.setReturnCourierCode(mustitReturnCourier.getCode()); // 반송 택배사 코드
                            orderDto.setReturnCourierName(returnCourierName); // 반송 택배사 이름
                        } else {
                            orderDto.setReturnCourierCode(MustitCourier.CUSTOM_COURIER_NAME.getCode()); // 반송 택배사 코드
                            orderDto.setReturnCourierCustomName(returnCourierName); // 직접입력한 반송 택배사 이름
                        }
                        orderDto.setReturnTrackingNumber(returnDeliveryInfoMap.get(orderUniqueId).getTrackingNumber()); // 반송 송장번호
                    }
                }
            }
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.CALCULATION_SCHEDULE) { // "정산예정 주문"
            Connection.Response calculationScheduleOrderPageResponse = mustitClient.getCalculationScheduleOrderListPage(headers, OLDEST_ORDER_DATE, today,0);
            int targetCalculationScheduleOrderCount = MustitHtmlParser.getCalculationScheduleOrderCount(calculationScheduleOrderPageResponse.body());
            if (targetCalculationScheduleOrderCount != 0) {
                int pageCount = getPageCount(targetCalculationScheduleOrderCount);
                for (int i = 0; i < pageCount; i++) {
                    int offset = ORDER_LIST_PAGE_SIZE * i;
                    Connection.Response orderPageResponse2 = mustitClient.getCalculationScheduleOrderListPage(headers, OLDEST_ORDER_DATE, today, offset);
                    // 교체 대상 정보 수집
                    MustitHtmlParser.collectOrderInfoForReplaceFromOrderListPage(orderPageResponse2.body(), mustitOrderInfoForReplace, MustitOrderStatusGroupType.CALCULATION_SCHEDULE);
                }

                // 주문상태 교체
                Map<String, String> completeOrderStatusMap = mustitOrderInfoForReplace.getOrderStatusMap();
                for (OrderDto.Request.CollectCallback orderDto: orderList) {
                    String orderUniqueId = orderDto.getOrderUniqueId();
                    if (completeOrderStatusMap.containsKey(orderUniqueId)) {
                        orderDto.setStatus(OrderDto.OrderStatus.getByCode(completeOrderStatusMap.get(orderUniqueId))); // 주문상태
                    }
                }
            }
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.COMPLETE) {
            Connection.Response completeOrderPageResponse = mustitClient.getCompleteOrderListPage(headers, completeOrderSearchStartDate, today, 0);
            int targetCompleteOrderCount = MustitHtmlParser.getCompleteOrderCount(completeOrderPageResponse.body());
            if (targetCompleteOrderCount != 0) {
                int pageCount = getPageCount(targetCompleteOrderCount);
                for (int i = 0; i < pageCount; i++) {
                    int offset = ORDER_LIST_PAGE_SIZE * i;
                    Connection.Response orderPageResponse2 = mustitClient.getCompleteOrderListPage(headers, completeOrderSearchStartDate, today, offset);
                    // 교체 대상 정보 수집
                    MustitHtmlParser.collectOrderInfoForReplaceFromOrderListPage(orderPageResponse2.body(), mustitOrderInfoForReplace, MustitOrderStatusGroupType.COMPLETE);
                }

                // 주문번호 교체
                Map<String, String> completeOrderIdMap = mustitOrderInfoForReplace.getOrderIdMap();
                for (OrderDto.Request.CollectCallback orderDto: orderList) {
                    String orderId = orderDto.getOrderId();
                    if (completeOrderIdMap.containsKey(orderId)) {
                        orderDto.setOrderId(completeOrderIdMap.get(orderId)); // 주문번호
                    }
                }
            }
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.ALL) {
            for (OrderDto.Request.CollectCallback orderDto: orderList) {
                String orderUniqueId = orderDto.getOrderUniqueId();

                // 주문자정보 세팅
                Connection.Response orderDetailGetResponse = mustitClient.getOrderDetailPage(headers, orderUniqueId);
                MustitOrderBuyer orderBuyer = MustitHtmlParser.getMustitOrderBuyer(orderDetailGetResponse.body());
                orderDto.setBuyerName(orderBuyer.getName()); // 주문자 이름
                orderDto.setBuyerEmail(orderBuyer.getEmail()); // 주문자 메일
                orderDto.setBuyerPhoneNumber(orderBuyer.getPhoneNumber()); // 주문자 전화
                orderDto.setBuyerMobilePhoneNumber(orderBuyer.getMobilePhoneNumber()); // 주문자 핸드폰
            }
        }
    }

    private int getPageCount(int totalCount) {
        return (totalCount%ORDER_LIST_PAGE_SIZE != 0) ? (totalCount/ORDER_LIST_PAGE_SIZE+1) : (totalCount/ORDER_LIST_PAGE_SIZE);
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.UpdateCallback> updateOrderToShop(String token, long jobId, OrderJobDto.Request.UpdateJob updateJobDto) {
        log.info("[updateOrderToShop()] START => jobId={}", jobId);

        OrderJobDto.Request.UpdateCallback orderJobDto = new OrderJobDto.Request.UpdateCallback();
        boolean successFlag = false;
        String resultMessage = null;
        Map<String, String> headers = new HashMap<>();
        headers.put("cookie", token);
        String shopOrderId = updateJobDto.getShopOrderId();
        String shopOrderUniqueId = updateJobDto.getShopUniqueOrderId();
        CourierDto courier = updateJobDto.getCourier();
        String trackingNumber = updateJobDto.getTrackingNumber();
        OrderJobDto.Request.OrderUpdateActionStatus orderUpdateActionStatus = updateJobDto.getStatus();
        String sellerMessage = updateJobDto.getSellerMessage();

        try {
            /////// 1. (주문전송 전) 현재 주문상태 가져오기
            MustitOrderStatus mustitOrderStatusBeforeUpdate = getCurrentOrderStatus(headers, shopOrderUniqueId);
            List<MustitOrderStatus> targetMustitOrderStatusList = MustitOrderStatus.getTargetStatusListByUpdateAction(mustitOrderStatusBeforeUpdate, orderUpdateActionStatus);

            if (mustitOrderStatusBeforeUpdate == null) {
                // 실패 처리
                resultMessage = "(주문전송 전) 알 수 없는 MUSTIT 주문상태";
            } else {
                /////// 2. 쇼핑몰에 있는 주문 업데이트
                Connection.Response orderUpdateResponse = null;
                Map<String, String> formData = new HashMap<>();

                if (targetMustitOrderStatusList.contains(mustitOrderStatusBeforeUpdate)) {
                    // 성공 처리
                    successFlag = true;
                    resultMessage = "이미 [" + mustitOrderStatusBeforeUpdate.getCode() + "] 상태";

                    if (Arrays.asList(MustitOrderStatus.DELIVERY, MustitOrderStatus.DELIVERY_BY_EXCHANGE).contains(mustitOrderStatusBeforeUpdate)) {
                        // 이미 머스트잇에서 해당 주문이 "배송중", "배송중(교환)" 상태로 있다면, 머스트잇에 있는 해당 주문의 배송정보(택배사, 송장번호) 업데이트
                        formData.put("number", shopOrderUniqueId);
                        formData.put("oversea_yn", "Y");
                        if (mustitOrderStatusBeforeUpdate == MustitOrderStatus.DELIVERY_BY_EXCHANGE) {
                            formData.put("exchange_yn", "Y");
                        }
                        formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                        formData.put("product_stats", "4");
                        formData.put("sub_status", "0");
                        formData.put("baesong_company", courier.getCode());
                        if (COURIER_NAME_DIRECT_INPUT_CODE.equals(courier.getCode())) {
                            formData.put("hidden_baesong", courier.getCustomName());
                        }
                        formData.put("songjang", trackingNumber);

                        orderUpdateResponse = mustitClient.updateOrderStatusToDelivery(headers, formData, shopOrderUniqueId);
                        if (MustitHtmlParser.isUpdateOrder(orderUpdateResponse.body(),
                                mustitOrderStatusBeforeUpdate, targetMustitOrderStatusList)) {
                            resultMessage += ". 배송정보 업데이트 성공";
                        } else {
                            resultMessage += ". " + MustitHtmlParser.getAlertMessage(orderUpdateResponse.body());
                        }
                    }
                } else {
                    if (mustitOrderStatusBeforeUpdate.isUpdatableTo(orderUpdateActionStatus)) { // 머스트잇 주문상태가 상태변경작업을 허용하는 경우
                        switch (orderUpdateActionStatus) {
                            case DELIVERY_READY:
                                // "발송요청"->"배송준비중" 업데이트
                                formData.put("order_sayoo_buyer", "");
                                formData.put("order_sayoo_seller", "");
                                orderUpdateResponse = mustitClient.updateOrderStatusToDeliveryReady(headers, formData, shopOrderUniqueId);
                                break;
                            case DELIVERY:
                                switch (mustitOrderStatusBeforeUpdate) {
                                    case PAYMENT_COMPLETE:
                                        // (1) "발송요청"->"배송중" 업데이트
                                        formData.put("number", shopOrderUniqueId);
                                        formData.put("oversea_yn", "Y");
                                        formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                        formData.put("product_stats", "4");
                                        formData.put("sub_status", "0");
                                        formData.put("baesong_company", courier.getCode());
                                        if (COURIER_NAME_DIRECT_INPUT_CODE.equals(courier.getCode())) {
                                            formData.put("hidden_baesong", courier.getCustomName());
                                        }
                                        formData.put("songjang", trackingNumber);
                                        orderUpdateResponse = mustitClient.updateOrderStatusToDelivery(headers, formData, shopOrderUniqueId);
                                        break;
                                    case DELIVERY_READY:
                                        // (2) "배송준비중"->"배송중" 업데이트
                                        formData.put("number", shopOrderUniqueId);
                                        formData.put("oversea_yn", "Y");
                                        formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                        formData.put("product_stats", "4");
                                        formData.put("sub_status", "1");
                                        formData.put("baesong_company", courier.getCode());
                                        if (COURIER_NAME_DIRECT_INPUT_CODE.equals(courier.getCode())) {
                                            formData.put("hidden_baesong", courier.getCustomName());
                                        }
                                        formData.put("songjang", trackingNumber);
                                        orderUpdateResponse = mustitClient.updateOrderStatusToDelivery(headers, formData, shopOrderUniqueId);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case EXCHANGE_CONFIRM:
                                switch (mustitOrderStatusBeforeUpdate) {
                                    case EXCHANGE_REQUEST:
                                        // 교환승인["교환요청"->"배송중(교환)" 업데이트] 하는 경우
                                    case PAYMENT_COMPLETE_BY_EXCHANGE:
                                        // 교환승인["발송요청(교환)"->"배송중(교환)" 업데이트] 하는 경우
                                    case DELIVERY_READY_BY_EXCHANGE:
                                        // 교환승인["배송준비중(교환)"->"배송중(교환)" 업데이트] 하는 경우
                                        formData.put("number", shopOrderUniqueId);
                                        formData.put("oversea_yn", "Y");
                                        formData.put("exchange_yn", "Y");
                                        formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                        formData.put("product_stats", "4");
                                        formData.put("sub_status", MustitOrderStatus.DELIVERY_READY_BY_EXCHANGE.equals(mustitOrderStatusBeforeUpdate) ? "1" : "0");
                                        formData.put("baesong_company", courier.getCode());
                                        if (COURIER_NAME_DIRECT_INPUT_CODE.equals(courier.getCode())) {
                                            formData.put("hidden_baesong", courier.getCustomName());
                                        }
                                        formData.put("songjang", trackingNumber);
                                        orderUpdateResponse = mustitClient.updateOrderStatusToDelivery(headers, formData, shopOrderUniqueId);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case EXCHANGE_REJECT:
                                // 교환거절["교환요청"->"배송중" 업데이트] 하는 경우
                                formData.put("gou_number", shopOrderId);
                                formData.put("jp_number", shopOrderUniqueId);
                                formData.put("oversea_yn", "N");
                                formData.put("type", "2");
                                formData.put("state", "2");
                                formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                formData.put("exchange_reject_reason", "기타");
                                formData.put("memo", sellerMessage);
                                formData.put("checkGuide", "on");
                                orderUpdateResponse = mustitClient.updateOrderStatusToDeliveryByReject(headers, formData, shopOrderUniqueId);
                                break;
                            case RETURN_CONFIRM:
                                // 반품승인["반품요청"->"반품환불완료"/"반품성사" 업데이트]
                                formData.put("number", shopOrderUniqueId);
                                formData.put("mode", "OrderReturn");
                                orderUpdateResponse = mustitClient.updateOrderStatusToReturnComplete(headers, formData, shopOrderUniqueId);
                                break;
                            case RETURN_REJECT:
                                // 반품거절["반품요청"->"배송중" 업데이트] 하는 경우
                                formData.put("gou_number", shopOrderId);
                                formData.put("jp_number", shopOrderUniqueId);
                                formData.put("oversea_yn", "N");
                                formData.put("type", "3");
                                formData.put("state", "2");
                                formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                formData.put("exchange_reject_reason", "기타");
                                formData.put("memo", sellerMessage);
                                formData.put("checkGuide", "on");
                                orderUpdateResponse = mustitClient.updateOrderStatusToDeliveryByReject(headers, formData, shopOrderUniqueId);
                                break;
                            case SELL_CANCEL:
                                switch (mustitOrderStatusBeforeUpdate) {
                                    case PAYMENT_COMPLETE:
                                        // "발송요청"->"판매취소완료" 업데이트
                                    case PAYMENT_COMPLETE_BY_EXCHANGE:
                                        // "발송요청(교환)"->"판매취소완료" 업데이트
                                    case DELIVERY_READY:
                                        // "배송준비중"->"판매취소완료" 업데이트
                                    case DELIVERY_READY_BY_EXCHANGE:
                                        // "배송준비중(교환)"->"판매취소완료" 업데이트
                                        formData.put("jp_number", shopOrderUniqueId);
                                        formData.put("cancel_reason", "6");
                                        formData.put("cancel_sayoo", sellerMessage);
                                        formData.put("cancle_chk", "on");
                                        orderUpdateResponse = mustitClient.updateOrderStatusToSellCancelComplete(headers, formData, shopOrderUniqueId);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case BUY_CANCEL_CONFIRM:
                                // 구매취소승인["구매취소요청"->"구매취소완료" 업데이트]
                                formData.put("cancel_ok", "on");
                                formData.put("temp_value", "dd");
                                orderUpdateResponse = mustitClient.updateOrderStatusToBuyCancelComplete(headers, formData, shopOrderUniqueId);
                                break;
                            case BUY_CANCEL_REJECT:
                                // 구매취소거절["구매취소요청"->"배송중"/"배송중(교환)" 업데이트]
                                formData.put("gou_number", shopOrderId);
                                formData.put("jp_number", shopOrderUniqueId);
                                formData.put("oversea_yn", "N");
                                formData.put("type", "1");
                                formData.put("state", "2");
                                formData.put("baesong_from", getMustitOrderDeliveryTypeCode(headers, shopOrderUniqueId));
                                formData.put("product_stats", "4");
                                formData.put("memo", sellerMessage);
                                formData.put("baesong_company", courier.getCode());
                                if (COURIER_NAME_DIRECT_INPUT_CODE.equals(courier.getCode())) {
                                    formData.put("hidden_baesong", courier.getCustomName());
                                }
                                formData.put("songjang", trackingNumber);
                                formData.put("checkGuide", "on");
                                orderUpdateResponse = mustitClient.updateOrderStatusToDeliveryByReject(headers, formData, shopOrderUniqueId);
                                break;
                            case CALCULATION_DELAY:
                                // "정산예정"->"정산보류중" 업데이트
                            case CALCULATION_SCHEDULE:
                                // "정산보류중"->"정산예정" 업데이트
                                boolean calculationDelayUpdateFlag = false;
                                for (MustitOrderStatus targetStatus : targetMustitOrderStatusList) {
                                    if (MustitOrderStatus.CALCULATION_DELAY.equals(targetStatus)) {
                                        calculationDelayUpdateFlag = true;
                                        break;
                                    }
                                }
                                formData.put("gou", shopOrderUniqueId);
                                formData.put("val", calculationDelayUpdateFlag ? "Y" : "N");
                                orderUpdateResponse = mustitClient.updateCalculationDelayFlag(headers, formData);
                                break;
                            default:
                                break;
                        }

                        // 주문 업데이트 결과 세팅
                        if (MustitHtmlParser.isUpdateOrder(orderUpdateResponse.body(),
                                mustitOrderStatusBeforeUpdate, targetMustitOrderStatusList)) {
                            successFlag = true;
                            resultMessage = mustitOrderStatusBeforeUpdate.getCode() + "->" + MustitOrderStatus.getCodesByStatusList(targetMustitOrderStatusList) + " 업데이트 성공";
                        } else {
                            resultMessage = MustitHtmlParser.getAlertMessage(orderUpdateResponse.body());
                        }
                    } else { // 머스트잇 주문상태가 상태변경작업을 허용하지 않는 경우
                        if (MustitOrderStatus.EXCHANGE_REQUEST.equals(mustitOrderStatusBeforeUpdate) &&
                                OrderJobDto.Request.OrderUpdateActionStatus.SELL_CANCEL.equals(orderUpdateActionStatus)) {
                            // "교환요청"->"판매취소완료" 업데이트인 경우는 머스트잇에서 허용하지 않으므로 "교환요청"->"발송요청(교환)"->2단계"판매취소완료" 순서로 업데이트해야 한다.

                            // 선수작업: "교환요청"->"발송요청(교환)" 업데이트
                            formData.put("number", shopOrderUniqueId);
                            orderUpdateResponse = mustitClient.updateOrderStatusToPaymentCompleteByExchange(headers, formData, shopOrderUniqueId);
                            if (MustitHtmlParser.isUpdateOrder(orderUpdateResponse.body(),
                                    mustitOrderStatusBeforeUpdate, Collections.singletonList(MustitOrderStatus.PAYMENT_COMPLETE_BY_EXCHANGE))) {
                                // 최종작업: "발송요청(교환)"->"판매취소완료" 업데이트
                                MustitOrderStatus mustitOrderStatusAfterExchangeConfirm = getCurrentOrderStatus(headers, shopOrderUniqueId);
                                formData.clear();
                                formData.put("jp_number", shopOrderUniqueId);
                                formData.put("cancel_reason", "6");
                                formData.put("cancel_sayoo", sellerMessage);
                                formData.put("cancle_chk", "on");
                                Connection.Response orderUpdateResponse2 = mustitClient.updateOrderStatusToSellCancelComplete(headers, formData, shopOrderUniqueId);
                                if (MustitHtmlParser.isUpdateOrder(orderUpdateResponse2.body(),
                                        mustitOrderStatusAfterExchangeConfirm, targetMustitOrderStatusList)) {
                                    successFlag = true;
                                    resultMessage = mustitOrderStatusBeforeUpdate.getCode() + "->" + MustitOrderStatus.getCodesByStatusList(targetMustitOrderStatusList) + " 업데이트 성공";
                                } else {
                                    resultMessage = MustitHtmlParser.getAlertMessage(orderUpdateResponse2.body());
                                }
                            } else {
                                resultMessage = MustitHtmlParser.getAlertMessage(orderUpdateResponse.body());
                            }
                        } else {
                            // 나머지 경우는 실패 처리
                            resultMessage = mustitOrderStatusBeforeUpdate.getCode() + "->" + orderUpdateActionStatus + " 업데이트 불가능";
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successFlag = false;
            resultMessage = "주문 업데이트 실패";
        }

        try {
            /////// 3. (주문 업데이트 성공여부와 상관 없이) 최종 주문정보 수집
            MustitOrderStatus mustitOrderStatusAfterUpdate = getCurrentOrderStatus(headers, shopOrderUniqueId);

            if (mustitOrderStatusAfterUpdate != null) {
                OrderDto.OrderStatus orderStatusAfterUpdate = OrderDto.OrderStatus.getByCode(mustitOrderStatusAfterUpdate.getCode());

                if (orderStatusAfterUpdate != null) {
                    OrderDto.Request.CollectCallback orderDto = new OrderDto.Request.CollectCallback();
                    orderDto.setOrderId(shopOrderId); // 주문번호
                    orderDto.setOrderUniqueId(updateJobDto.getShopUniqueOrderId()); // 주문고유번호
                    orderDto.setStatus(orderStatusAfterUpdate); // 주문상태

                    // 진행주문 관련 구매자 클래임 사유 가져오기
                    Map<String, String> orderUniqueIdToBuyerClaimReasonMap = getOnGoingOrderBuyerClaimReason(headers);

                    switch (orderStatusAfterUpdate) {
                        case DELIVERY:
                            // "배송중"인 경우에는 배송정보 수집
                            Connection.Response orderDeliveryInfoPageResponse2 = mustitClient.getOrderDeliveryInfoPage(headers, shopOrderUniqueId);
                            MustitDeliveryInfo mustitDeliveryInfo2 = MustitHtmlParser.getDeliveryInfo(orderDeliveryInfoPageResponse2.body());
                            String deliveryCourierName = mustitDeliveryInfo2.getCourierName();
                            orderDto.setCourierCode(Objects.requireNonNullElse(MustitCourier.getByName(deliveryCourierName), MustitCourier.CUSTOM_COURIER_NAME).getCode()); // 택배사 코드
                            orderDto.setCourierName(deliveryCourierName); // 택배사 이름
                            orderDto.setTrackingNumber(mustitDeliveryInfo2.getTrackingNumber()); // 송장번호
                            orderDto.setDeliveryType(mustitDeliveryInfo2.getDeliveryType()); // 배송종류
                            break;
                        case DELIVERY_BY_EXCHANGE:
                            // "배송중(교환)"인 경우에는 교환배송정보 수집
                            Connection.Response orderExchangeDeliveryInfoPageResponse = mustitClient.getExchangeOrderDeliveryInfoPage(headers, shopOrderUniqueId);
                            MustitDeliveryInfo mustitExchangeDeliveryInfo = MustitHtmlParser.getDeliveryInfo(orderExchangeDeliveryInfoPageResponse.body());
                            String exchangeDeliveryCourierName = mustitExchangeDeliveryInfo.getCourierName();
                            orderDto.setExchangeCourierCode(Objects.requireNonNullElse(MustitCourier.getByName(exchangeDeliveryCourierName), MustitCourier.CUSTOM_COURIER_NAME).getCode()); // 교환 택배사 코드
                            orderDto.setExchangeCourierName(exchangeDeliveryCourierName); // 교환 택배사 이름
                            orderDto.setExchangeTrackingNumber(mustitExchangeDeliveryInfo.getTrackingNumber()); // 교환 송장번호
                            break;
                        case EXCHANGE_REQUEST:
                            // "교환요청"인 경우에는 교환요청사유 세팅
                            orderDto.setExchangeReason(
                                    orderUniqueIdToBuyerClaimReasonMap.get(orderDto.getOrderUniqueId()));
                            break;
                        case RETURN_REQUEST:
                            // "반품요청"인 경우에는 반품요청사유 세팅
                            orderDto.setReturnReason(
                                    orderUniqueIdToBuyerClaimReasonMap.get(orderDto.getOrderUniqueId()));
                            break;
                        case BUY_CANCEL_REQUEST:
                            // "구매취소요청"인 경우에는 구매취소사유 세팅
                            orderDto.setPurchaseCancellationReason(
                                    orderUniqueIdToBuyerClaimReasonMap.get(orderDto.getOrderUniqueId()));
                            break;
                        default:
                            break;
                    }

                    // 주문대화 수집
                    OrderBaseConversationDto orderBaseConversationDto = getOrderBaseConversation(headers, shopOrderId, shopOrderUniqueId);
                    if (orderBaseConversationDto != null) {
                        orderDto.getOrderBaseConversationList().add(orderBaseConversationDto);
                    }

                    // 최종 주문정보 저장
                    orderJobDto.getOrderList().add(orderDto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        orderJobDto.getJobTaskResponseBaseDto().setJobId(jobId);
        orderJobDto.getJobTaskResponseBaseDto().setRequestId(updateJobDto.getShopAccount().getRequestId());
        orderJobDto.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        orderJobDto.getJobTaskResponseBaseDto().setMessage(resultMessage);
        orderJobDto.setShopAccount(modelMapper.map(updateJobDto.getShopAccount(), ShopAccountDto.Response.class));

        return new AsyncResult<>(orderJobDto);
    }

    /**
     * 해당 주문건에 대한 주문타입코드를 구한다
     */
    private String getMustitOrderDeliveryTypeCode(Map<String, String> headers, String shopOrderUniqueId) throws IOException {
        Connection.Response orderDeliveryInfoPageResponse = mustitClient.getOrderDeliveryInfoPage(headers, shopOrderUniqueId);
        MustitDeliveryInfo mustitDeliveryInfo = MustitHtmlParser.getDeliveryInfo(orderDeliveryInfoPageResponse.body());

        return MustitDeliveryType.getByName(mustitDeliveryInfo.getDeliveryType()).getCode();
    }

    /**
     * 해당 주문건에 대한 현재 주문상태를 구한다
     */
    private MustitOrderStatus getCurrentOrderStatus(Map<String, String> headers, String orderUniqueId) throws IOException {
        Connection.Response orderDetailPageResponse = mustitClient.getOrderDetailPage(headers, orderUniqueId);
        String currentOrderStatus =
                MustitHtmlParser.getOrderStatus(orderDetailPageResponse.body(), orderUniqueId);

        if ("구매완료".equals(currentOrderStatus)) {
            // "정산예정"인지 "정산보류중"인지 구분하기 위해 주문상태 교체
            OrderDto.Request.CollectCallback calculationScheduleOrder = new OrderDto.Request.CollectCallback();
            calculationScheduleOrder.setOrderUniqueId(orderUniqueId);
            List<OrderDto.Request.CollectCallback> calculationScheduleOrderList = new ArrayList<>();
            calculationScheduleOrderList.add(calculationScheduleOrder);
            replaceOrderInfo(headers, calculationScheduleOrderList, MustitOrderStatusGroupType.CALCULATION_SCHEDULE, null);

            switch (calculationScheduleOrderList.get(0).getStatus()) {
                case CALCULATION_SCHEDULE:
                    currentOrderStatus = "정산예정";
                    break;
                case CALCULATION_DELAY:
                    currentOrderStatus = "정산보류중";
                    break;
                default:
                    break;
            }
        }

        return MustitOrderStatus.getByCode(currentOrderStatus);
    }

    /**
     * 해당 주문건에 대한 주문대화를 구한다
     */
    private OrderBaseConversationDto getOrderBaseConversation(Map<String, String> headers, String orderId, String orderUniqueId) throws IOException {
        Connection.Response orderConversationResponse = mustitClient.getOrderConversation(headers, orderUniqueId);
        List<OrderBaseConversationMessageDto> orderConversationMessageList = MustitHtmlParser.getOrderConversationList(orderConversationResponse.body());

        if (orderConversationMessageList.isEmpty()) {
            return null;
        }

        OrderBaseConversationDto orderBaseConversationDto = new OrderBaseConversationDto();
        orderBaseConversationDto.setOrderBaseConversationMessageList(orderConversationMessageList);
        orderBaseConversationDto.setOrderId(orderId);
        orderBaseConversationDto.setOrderUniqueId(orderUniqueId);
        orderBaseConversationDto.setChannelId(orderId);

        return orderBaseConversationDto;
    }

    private void setOrderCommonData(MustitOrderStatusGroupType mustitOrderStatusGroupType, OrderDto.Request.CollectCallback orderDto, Elements data) {
            orderDto.setOrderId(data.get(0).text()); // 주문번호
            orderDto.setOrderUniqueId(data.get(1).text()); //주문고유번호
            orderDto.setOrderDate(LocalDateTime.parse(data.get(2).text(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); // 결제일시
            orderDto.setPostId(data.get(3).text()); // 상품번호
            orderDto.setProductName(data.get(4).text()); // 상품명

            // 선택사항 정보로부터 메모/색상/사이즈 구하기
            String optionInfo = data.get(5).text();
            if (!StringUtils.isEmpty(optionInfo)) {
                orderDto.setMemo(optionInfo); // 선택사항 텍스트
                String[] options = optionInfo.split(" / ");
                for (String option : options) {
                    String[] strings = option.split(" : ");
                    String key = strings[0];
                    String value = strings[1];

                    if ("색상".equals(key)) {
                        orderDto.setClassificationValue(value); // 색상
                    } else if ("사이즈".equals(key)) {
                        orderDto.setOptionName(value); // 사이즈
                    }
                }
            } else {
                orderDto.setMemo("옵션없음"); // 선택사항 텍스트
            }

            orderDto.setPrice(Long.valueOf(data.get(6).text())); // 판매가격
            orderDto.setQuantity(data.get(7).text()); // 수량
            orderDto.setDeliveryFeeType(data.get(8).text()); // 배송비 유형
            orderDto.setDeliveryFee(data.get(9).text()); // 배송비
            orderDto.setPaymentPrice(Long.valueOf(data.get(10).text())); // 결제금액
            orderDto.setBuyerName(data.get(11).text()); // 주문자 이름
            orderDto.setBuyerId(data.get(12).text()); // 주문자 아이디

        if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.ONGOING) {
            orderDto.setRecipientName(data.get(13).text()); // 수취인 이름
            orderDto.setRecipientMobilePhoneNumber(data.get(14).text()); // 수취인 핸드폰
            // 수취인 전화번호 구하기
            String recipientNumber = data.get(15).text().replace("--", "").trim();
            if (!StringUtils.isEmpty(recipientNumber)) {
                orderDto.setRecipientPhoneNumber(recipientNumber); // 수취인 전화번호
            }
            orderDto.setRecipientZipCode(data.get(16).text()); // 우편번호
            orderDto.setRecipientAddress(data.get(17).text()); // 주소
            orderDto.setDeliveryMessage(data.get(18).text()); // 배송 요청사항
            orderDto.setStatus(OrderDto.OrderStatus.getByCode(data.get(20).text())); // 주문상태
        } else if (mustitOrderStatusGroupType == MustitOrderStatusGroupType.COMPLETE) {
            orderDto.setStatus(OrderDto.OrderStatus.getByCode(data.get(14).text())); // 주문상태
        }

    }

    private String getRequiredCookie(Connection.Response response){
        return PatternExtractor.MUSTIT_SESSIONID_FULL_STR.extract(response.header("Set-Cookie"));
    }

    private List<Pair<String, String>> mapToMustitSaleFormData(OnlineSaleDto onlineSaleDto, MustitSale oldMustitSale) throws Exception {
        List<Pair<String, String>> data = new ArrayList<>();

        // 카테고리 항목
        ShopCategoryDto genderCategory = onlineSaleDto.getShopSale().getShopCategory();
        ShopCategoryDto largeCategory = genderCategory.getChild();
        ShopCategoryDto mediumCategory = largeCategory.getChild();
        ShopCategoryDto smallCategory = mediumCategory.getChild();
        String smallCategoryCode = null;

        //  카테고리 필터 항목
        String filters = "";
        String filterExistenceFlag = "0";
        if (smallCategory != null) {
            smallCategoryCode = smallCategory.getShopCategoryCode();
            if (smallCategory.getFilter() != null) {
                filters = smallCategory.getFilter();
                filterExistenceFlag = "1";
            }
        } else if (mediumCategory.getFilter() != null) {
            filters = mediumCategory.getFilter();
            filterExistenceFlag = "1";
        }

        // 색상-사이즈별 판매수량 목록
        List<MustitProductOptionStock> productOptionStockList = new ArrayList<>();
        for (ProductDto productDto : onlineSaleDto.getProductList()) {
            for (ProductOptionDto productOptionDto : productDto.getProductOptionList()) {
                productOptionStockList.add(new MustitProductOptionStock(
                        productDto.getClassificationValue() + "|" + productOptionDto.getName(),
                        String.valueOf(productOptionDto.getQuantity())));
            }
        }
        for (MustitProductOptionStock stock : productOptionStockList) {
            data.add(Pair.of("opt[]", stock.getOptionName()));
            data.add(Pair.of("stock[]", stock.getOptionQuantity()));
        }

        // 상품 이미지
        onlineSaleDto.getSaleImageList().sort(new MustitSaleImageListSort()); // 대표 이미지가 첫번째 순서로 오게 정렬
        List<String> mustitUploadedProductImageUrlList = new ArrayList<>(); // 머스트잇에 업로드된 상품 이미지 url 목록
        for (int i = 0; i < onlineSaleDto.getSaleImageList().size(); i++) {
            MustitProductImage mustitProductImage = new MustitProductImage(
                    "0",
                    String.valueOf(onlineSaleDto.getSaleImageList().get(i).getOriginImagePath())
            );
            String[] splitImageUrl = mustitProductImage.getSrc().split("/");
            String fileNameWithExtension = splitImageUrl[splitImageUrl.length - 1];
            String[] splitFileNameWithExtension = fileNameWithExtension.split("\\.");
            String extension = splitFileNameWithExtension[splitFileNameWithExtension.length - 1];
            InputStream inputStream = getImageInputStream(mustitProductImage.getSrc(), extension);

            if (oldMustitSale != null && i == 0) { // 판매 수정 시 기존 판매글 관련 상품 이미지에 대한 처리
                // 상품 대표 이미지 변경 요청
                Connection.Response productMainImageUpdateResponse = mustitClient.updateProductMainImage(fileNameWithExtension, inputStream, oldMustitSale.getSaleId());
                String mustitUploadedMainImageUrl = productMainImageUpdateResponse.body();
                log.debug("[mapToMustitSaleFormData()] mustitUploadedMainImageUrl => \n{}", mustitUploadedMainImageUrl);

                // 기존 상품의 대표 이미지는 유지하고 비대표 이미지는 삭제 처리
                for (int j = 0; j < oldMustitSale.getImageList().size(); j++) {
                    MustitProductImage oldImage = oldMustitSale.getImageList().get(j);
                    String deleteFlag = (j == 0) ? "0" : "1";
                    data.add(Pair.of("delete_img[]", deleteFlag));
                    data.add(Pair.of("imgSrc[]", oldImage.getSrc()));
                }

                // 리스트에 추가
                mustitUploadedProductImageUrlList.add(mustitUploadedMainImageUrl);
            } else {
                Connection.Response imageUploadResponse = mustitClient.uploadProductImage(fileNameWithExtension, inputStream);
                String mustitUploadedProductImageUrl = imageUploadResponse.body();
                data.add(Pair.of("delete_img[]", mustitProductImage.getDeleteFlag()));
                data.add(Pair.of("imgSrc[]", imageUploadResponse.body()));

                // 리스트에 추가
                mustitUploadedProductImageUrlList.add(mustitUploadedProductImageUrl);
            }
        }

        //MustitSale Object 데이타 구성(내용은 Dto 참고)
        data.add(Pair.of("brand", onlineSaleDto.getBrandMap().getSourceCode()));
        data.add(Pair.of("brandnmh", onlineSaleDto.getBrandMap().getSourceName()));
        data.add(Pair.of("category_flag", genderCategory.getShopCategoryCode()));
        data.add(Pair.of("category", largeCategory.getShopCategoryCode()));
        data.add(Pair.of("company", mediumCategory.getShopCategoryCode()));
        if (!StringUtils.isEmpty(smallCategoryCode)) {
            data.add(Pair.of("type", smallCategoryCode));
        }
        data.add(Pair.of("filters", filters));
        data.add(Pair.of("filter_yn", filterExistenceFlag));
        data.add(Pair.of("product_name", onlineSaleDto.getSubject()));
        data.add(Pair.of("memo", ""));
        data.add(Pair.of("option_delivery", "no"));
        data.add(Pair.of("product_sangtae", MustitProductCondition.valueOf(onlineSaleDto.getCondition().getName()).getCode()));
        data.add(Pair.of("wonsanji_text", onlineSaleDto.getProductionCountry()));
        data.add(Pair.of("bonus_option", ""));
        data.add(Pair.of("hongbo", ""));
        data.add(Pair.of("sales_type", MustitImportType.valueOf(onlineSaleDto.getImportType().getName()).getCode()));
        data.add(Pair.of("kc_target", "4"));
        data.add(Pair.of("sijoong_price_tmp", String.valueOf(onlineSaleDto.getPrice())));
        data.add(Pair.of("point_tmp", "0"));
        data.add(Pair.of("cupon_price_tmp", "0"));
        data.add(Pair.of("baesong_from", MustitDeliveryType.valueOf(onlineSaleDto.getSaleMustit().getDeliveryType()).getCode()));
        data.add(Pair.of("baesong_jumin", "0"));
        data.add(Pair.of("customs_duties", "N"));
        data.add(Pair.of("baesong_kind", "normal"));
        data.add(Pair.of("baesong_type", MustitDeliveryFeeType.valueOf(onlineSaleDto.getSaleMustit().getDeliveryFeeType()).getCode()));
        data.add(Pair.of("baesongbi", String.valueOf(onlineSaleDto.getSaleMustit().getDeliveryFee())));
        data.add(Pair.of("deliveryExceptionChoice", "N"));
        data.add(Pair.of("bundle_delivery_type", "1"));
        data.add(Pair.of("deliveryFeeUseEach", "N"));
        data.add(Pair.of("premium",MustitPremiumSalePeriod.getCodeByPeriod(onlineSaleDto.getSaleMustit().getPremiumSalePeriod())));
        data.add(Pair.of("bold", MustitBoldFontPeriod.getCodeByPeriod(onlineSaleDto.getSaleMustit().getBoldFontPeriod())));

        // 상세설명
        String content = onlineSaleDto.getSaleMustit().getDetail().replace("\n", "<br>");
        data.add(Pair.of("ir1", appendAdditionalContentToSaleDescription(content, mustitUploadedProductImageUrlList, createProductInformationNoticeText(onlineSaleDto))));

        return data;
    }

    private InputStream getImageInputStream(String imageUrl, String extension) throws IOException {
        BufferedImage originalImage = ImageIO.read(new URL(imageUrl));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, extension, outputStream);
        outputStream.flush();
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 진행주문 관련 구매자 클레임 사유를 가져온다
     */
    private Map<String, String> getOnGoingOrderBuyerClaimReason(Map<String, String> headers) throws IOException {
        Map<String, String> orderUniqueIdToBuyerClaimReasonMap = new HashMap<>();

        // 교환반품 요청 사유 수집
        Connection.Response exchangeReturnRequestOrderListResponse = mustitClient.getExchangeReturnRequestOrderList(headers);
        Elements exchangeReturnRequestOrderListWorkbook = exchangeReturnRequestOrderListResponse.parse().body().getElementsByTag("workbook");
        Elements exchangeReturnRequestOrderListRows = exchangeReturnRequestOrderListWorkbook.select("row");
        exchangeReturnRequestOrderListRows.remove(0); // 컬럼명이 있는 row는 제거
        if (!CollectionUtils.isEmpty(exchangeReturnRequestOrderListRows)) {
            for (Element row : exchangeReturnRequestOrderListRows) {
                Elements data = row.getElementsByTag("data");
//                String orderId = data.get(0).text(); // 주문번호
                String orderUniqueId = data.get(1).text(); //주문고유번호
                String exchangeReturnRequestReason = data.get(21).text(); // 교환반품 요청 사유

                // 맵에 추가
                orderUniqueIdToBuyerClaimReasonMap.put(orderUniqueId, exchangeReturnRequestReason);
            }
        }

        // 구매취소 요청 사유 수집
        Connection.Response buyCancelRequestOrderListResponse = mustitClient.getBuyCancelRequestOrderList(headers);
        Elements buyCancelRequestOrderListWorkbook = buyCancelRequestOrderListResponse.parse().body().getElementsByTag("workbook");
        Elements buyCancelRequestOrderListRows = buyCancelRequestOrderListWorkbook.select("row");
        exchangeReturnRequestOrderListRows.remove(0); // 컬럼명이 있는 row는 제거
        if (!CollectionUtils.isEmpty(buyCancelRequestOrderListRows)) {
            for (Element row : buyCancelRequestOrderListRows) {
                Elements data = row.getElementsByTag("data");
//                String orderId = data.get(0).text(); // 주문번호
                String orderUniqueId = data.get(1).text(); //주문고유번호
                String purchaseCancelRequestReason = data.get(21).text(); // 구매취소 요청 사유

                // 맵에 추가
                orderUniqueIdToBuyerClaimReasonMap.put(orderUniqueId, purchaseCancelRequestReason);
            }
        }

        return orderUniqueIdToBuyerClaimReasonMap;
    }

    /**
     * 상품 이미지 목록에서 대표 이미지가 가장 첫번째 순서에 위치하도록 정렬
     */
    private static class MustitSaleImageListSort implements Comparator<OnlineSaleImageDto> {

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
     *
     * @return
     * @throws IOException
     */
    public String collectShopNotice()
            throws IOException {
        log.info("CALL collectShopNotice");
        return mustitClient.getShopNotice().body();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String collectShopNoticeDetail(String noticeId)
            throws IOException {
        log.info("CALL collectShopNoticeDetail");
        Map<String, String> data = new HashMap<>();
        data.put("number", noticeId);
        return mustitClient.getShopNoticeDetail(data).body();
    }


    /**
     * 판매글 내용에 이미지 목록 넣기
     */
    private String appendAdditionalContentToSaleDescription(String content, List<String> imageUrlList, String productInformationNotice) {
        StringBuilder result = new StringBuilder();
        result.append(content);
        for (String imageUrl : imageUrlList) {
            result.append(String.format("<p style=\"text-align:center;\"><img src=\"%s\"></p>", imageUrl));
        }
        result.append(productInformationNotice);
        return result.toString();
    }

    /**
     * 머스트잇 판매글 상태를 구한다
     */
    private MustitSaleStatus getMustitSaleStatus(Map<String, String> headers, String saleId) throws IOException {
        MustitSaleStatus result;

        // 해당 판매글번호에 대해 품절상품관리 페이지에서 조회한다
        Connection.Response soldOutPageResponse = mustitClient.getSaleSoldOutPage(headers, saleId);
        result = MustitHtmlParser.parseMustitSaleStatusBySoldOutPage(soldOutPageResponse.body());
        if (result == null) {
            // 해당 판매글번호에 대해 판매상품관리 페이지에서 조회한다
            Connection.Response sellingPageResponse = mustitClient.getSaleSellingPage(headers, saleId);
            result = MustitHtmlParser.getMustitSaleStatusBySellingPage(sellingPageResponse.body());
        }

        return result != null ? result : MustitSaleStatus.NOT_FOUND_SALE;
    }

    /**
     * 판매상태 변경 성공 여부를 구한다
     */
    private boolean isSaleStatusUpdated(String message) {
        boolean result = false;

        if (message.contains("판매중지 처리되었습니다") ||
                message.contains("판매중지해제 처리되었습니다")) {
            result = true;
        }

        return result;
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

        text.append("<br><상품정보고시><br><p>");
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

    /**
     * 판매삭제 성공 여부를 구한다
     */
    public static boolean isSaleDeleted(String html) {

        return html.contains("등록된 상품이 삭제되었습니다");
    }

}
