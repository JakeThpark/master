package com.wanpan.app.service.feelway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.PatternExtractor;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.feelway.*;
import com.wanpan.app.dto.job.*;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.ShopAccountToken;
import com.wanpan.app.exception.InvalidRequestException;
import com.wanpan.app.repository.JobRepository;
import com.wanpan.app.service.ComparisonCheckResult;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.feelway.parser.*;
import com.wanpan.app.service.job.JobService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FeelwayService implements ShopService {
    private FeelwayRequestPageService feelwayRequestPageService;
    private JobRepository jobRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ShopAccountDto.Response checkSignIn(String accountId, String password,
                                               ShopAccountDto.Response shopAccountResponseDto) {

        shopAccountResponseDto.setSuccessFlag(false);
        try {
            String token = getToken(accountId, password, ShopAccountToken.Type.SESSION);

            if (Objects.isNull(token)) {
                return shopAccountResponseDto;
            }

            shopAccountResponseDto.setSuccessFlag(true);
            return shopAccountResponseDto;
        } catch (InvalidRequestException e) {
            log.warn(e.getMessage() + Arrays.toString(e.getStackTrace()));
            shopAccountResponseDto.setMessage(e.getMessage());
        }

        return shopAccountResponseDto;
    }

    @Override
    public boolean isKeepSignIn(String token, String accountId, ShopAccountToken.Type tokenType) {
        try {
            String body = feelwayRequestPageService.requestSignInCheckPage(token);
            return FeelwaySignInParser.isKeepSignIn(body, accountId);
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getToken(String accountId, String password, ShopAccountToken.Type tokenType) {
        try {
            String token = feelwayRequestPageService.requestSession();

            FeelwaySignIn feelwaySignIn = new FeelwaySignIn(accountId, password);
            String body = feelwayRequestPageService.requestSignIn(feelwaySignIn, token);
            FeelwaySignInParser.assertLoginSuccess(body, feelwaySignIn);

            return token;
        } catch (IOException e) {
            // todo
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BrandDto> getBrandList(String token) {
        try {
            String body = feelwayRequestPageService.requestRegisterProductPage(token);
            return FeelwayBrandParser.getBrands(body);
        } catch (IOException e) {
            // todo 해당 에러는 job 테이블의 상태 데이터를 변경해야 한다.
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    /**
     * 판매글 등록
     * @param token
     * @param job
     * @param onlineSaleDto
     * @return
     */
    @Async
    @Override
    public ListenableFuture<RegisterDto.Response> postSaleToShop(String token, Job job, OnlineSaleDto onlineSaleDto) {
        log.info("===============Feelway registerSaleToShop Call-jobId:{}",job.getId());
        log.info("===============JobService.processingSaleList: {}-jobId:{}",JobService.processingSaleList,job.getId());
        String productId = "";
        String message = "";
        boolean successFlag = false;
        try {
            int tryCount = 0;
            while(true) { //재시도를 위한 루프
                log.info("===============Feelway registerSaleToShop Call SLEEP END-jobId:{}",job.getId());
                log.info("===============JobService.processingSaleList: {}-jobId:{}",JobService.processingSaleList,job.getId());

                //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                tryCount++;
                //글 등록전에 현재 등록된 수를 읽어온다.
                int beforeSellingProductCount = FeelwayProductParser.getSellingProductCount(feelwayRequestPageService.getSellingProduct(token));

                //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 글 등록시 기본으로 넘겨줘야 하는 값을 가져오기 위해 판매 페이지 조회
                String registerProductPage = feelwayRequestPageService.requestRegisterProductPage(token);
                FeelwayProductForCreate feelwayProduct =
                        FeelwayProductParser.getFeelwayProductForCreate(registerProductPage);

                //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 이미지 업로드
                String imageUploadId = FeelwayProductParser.getImageUploadId(registerProductPage);
                List<String> imageUrlList = getImagePathList(onlineSaleDto);
                String uploadResultHtml = feelwayRequestPageService.requestFileUpload(token, imageUploadId, imageUrlList);
                FeelwayProduct feelwayProductImage = FeelwayImageUploadParser.getImages(uploadResultHtml);

                //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 기본 데이터에 판매글 데이터 합침
                feelwayProduct.setImage(feelwayProductImage);
                setDtoToFeelwayProductForCreate(feelwayProduct, onlineSaleDto);

                // 판매글 등록
                String registeredHtml = feelwayRequestPageService.registerProduct(token, feelwayProduct);

                // 등록된 판매글 ID 가져오기
                productId = FeelwayAfterRegisterProductParser.getProductId(registeredHtml);


                //등록된 판매글의 ID가 없거나 0일 경우 확인 처리를 수행하고 재시도를 한다.
                if (StringUtils.isEmpty(productId) || productId.equals("0")) {
                    log.error("===============Not productId Case, jobId:{}", job.getId());

                    //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //현재 등록된 수를 읽어온다.
                    int afterSellingProductCount = FeelwayProductParser.getSellingProductCount(feelwayRequestPageService.getSellingProduct(token));

                    //이전 개수 + 1 이 현재 개수일 경우 판매 등록된걸로 간주하고 연결 프로세싱을 수행한다.
                    if (afterSellingProductCount == beforeSellingProductCount + 1) {

                        //중복 요청시 문제를 위해 5초 대기 후 등록한다.
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //판매중 상품목록의 제일 마지막을 가져와서 매핑 가능한지 비교한 후 가능할 경우는 postId를 연결한다.
                        FeelwaySellingProduct feelwaySellingProduct = FeelwayProductParser.getlatestSellingProduct(feelwayRequestPageService.getSellingProduct(token));
                        if(!ObjectUtils.isEmpty(feelwaySellingProduct)
                                && feelwaySellingProduct.getBrand().equals(onlineSaleDto.getBrandMap().getSourceName())
                                && feelwaySellingProduct.getSubject().equals(onlineSaleDto.getSubject())
                        ){
                            //매핑 가능 case
                            log.error("ooooooooooo Feelway Mapping Enable - {},jobId:{}", feelwaySellingProduct.getProductNumber(),job.getId());
                            productId = feelwaySellingProduct.getProductNumber();
                            //성공으로 기록하고 재시도 루프를 나간다
                            successFlag = true;
                            break;

                        }else{

                            log.error("ooooooooooo Feelway Mapping Disable Retry({}) - {},jobId:{}",tryCount, feelwaySellingProduct,job.getId());
                            //가져온 제품이 없을 경우는 데이타 오류 재시도 수행
                            if(tryCount < 2) continue;

                        }
                    } else { //아닐 경우 재시도 로직을 수행한다.(같은경우-등록안된경우, 다른경우-확인이불가한경우)
                        log.error("ooooooooooo Not register Case Retry({})!!!jobId:{}", tryCount,job.getId());
                        //재시도 횟수가 2번 미만일 경우에 재시도를 수행한다.
                        if(tryCount < 2) continue;
                    }

                    log.error("ooooooooooo Register Failed!! jobId-{},productId-{}, tryCount: {}, registeredHtml: {}", job.getId(), productId, tryCount, registeredHtml);
                    successFlag = false;
                } else {
                    successFlag = true;
                }
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();//todo
            message = e.getMessage();
        }finally {
            log.info("===============Feelway postSaleToShop JOB COMPLETED => jobId={}, successFlag:{}\n" +
                    "Feelway postSaleToShop JobService.processingSaleList={}", job.getId(), successFlag, JobService.processingSaleList);
            //해당 account의 job을 사용 종료한다.
            JobService.processingSaleList.remove(onlineSaleDto.getShopSale().getShopAccount().getLoginId());
        }

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                productId,
                ShopSaleJobDto.SaleStatus.ON_SALE,
                job.getId(),
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message
        ));
    }

    private List<String> getImagePathList(OnlineSaleDto onlineSaleDto) {
        return onlineSaleDto.getSaleImageList().stream()
                        .map((OnlineSaleImageDto::getOriginImagePath))
                        .collect(Collectors.toList());
    }

    /**
     * 현재 판매글 상태를 읽어와서 수정 가능 여부를 판단한다
     * 판매글 정보를 업데이트 한다.
     * @param token
     * @param jobId
     * @param onlineSaleDto
     * @return
     */
    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleToShop(String token, long jobId,
                                                                   OnlineSaleDto onlineSaleDto) {
        log.info("Feelway updateSaleToShop JOB START => jobId={}", jobId);
        String message = "";
        boolean successFlag = false;
        ShopSaleJobDto.SaleStatus saleStatus = onlineSaleDto.getShopSale().getStatus();
        try {
            //해당 상품의 상태를 읽어 온다..
            FeelwaySellingProduct feelwaySellingProduct = getShopSaleStatudByProductNumber(token, onlineSaleDto.getShopSale().getPostId());

            if(ObjectUtils.isEmpty(feelwaySellingProduct)){
                //상품이 존재하지 않는경우(에러일 경우는 exception으로 빠진다.)
                log.error("Product Not Found - NOT_FOUND_SALE");
                successFlag = false;
                saleStatus = ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE;
                message =  "Product Not Found - Request is " + saleStatus.name();

            }else if(feelwaySellingProduct.getSaleStatus() == ShopSaleJobDto.SaleStatus.SALE_STOP
                || feelwaySellingProduct.getSaleStatus() == ShopSaleJobDto.SaleStatus.SALE_HOLD){
                //판매중지, 판매보류의 경우 실패지만 현재 상태를 내려준다
                saleStatus = feelwaySellingProduct.getSaleStatus();
                successFlag = false;
                message = "수정 불가";
            }else{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String updateProductPage = feelwayRequestPageService.requestUpdateProductPage(token, onlineSaleDto);
                FeelwayProductForUpdate feelwayProduct =
                        FeelwayProductParser.getFeelwayProductForUpdate(updateProductPage);
                String imageUploadId = FeelwayProductParser.getImageUploadId(updateProductPage);
                List<String> imageUrlList = getImagePathList(onlineSaleDto);
                feelwayProduct.setUpdatePhotoCount(imageUrlList.size());

                String uploadResultHtml = feelwayRequestPageService.requestFileUpload(token, imageUploadId, imageUrlList);
                FeelwayProduct feelwayProductImage = FeelwayImageUploadParser.getImages(uploadResultHtml);
                feelwayProduct.setImage(feelwayProductImage);

                setDtoToFeelwayProductForUpdate(feelwayProduct, onlineSaleDto);
                feelwayRequestPageService.updateProduct(token, feelwayProduct);
                //해당 상품의 상태를 읽어 온다.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                feelwaySellingProduct = getShopSaleStatudByProductNumber(token, onlineSaleDto.getShopSale().getPostId());
                saleStatus = feelwaySellingProduct.getSaleStatus();

                successFlag = true;
                message = "수정 성공";
            }
        } catch (IOException e) {
            e.printStackTrace();
            successFlag = false;
            message = e.getMessage();
        }

        log.info("Feelway updateSaleToShop JOB COMPLETED => jobId={}", jobId);

        return new AsyncResult<>(new RegisterDto.Response(
                onlineSaleDto.getShopSale().getId(),
                onlineSaleDto.getShopSale().getPostId(),
                saleStatus,
                jobId,
                onlineSaleDto.getShopSale().getId(),
                successFlag,
                message
        ));
    }

    @Async
    @Override
    public ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String token, long jobId,
                                                                                      ShopQnaJobDto.QuestionStatus questionStatus,
                                                                                      ShopAccountDto.Request request) {
        try {
            log.info("Call collectQna");
            String response = feelwayRequestPageService.collectQna(token, questionStatus, null);
            List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>(FeelwayQnAParser.parseQna(response));
            shopQnAList.sort((p1, p2) -> p2.getQuestionId().compareTo(p1.getQuestionId()));

            //최종 response 구성
            ShopQnaJobDto.Request.CollectCallback collectShopQnAListCallback = new ShopQnaJobDto.Request.CollectCallback();
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setJobId(jobId);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setSuccessFlag(true);
            collectShopQnAListCallback.getJobTaskResponseBaseDto().setMessage("수집성공");
            collectShopQnAListCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
            collectShopQnAListCallback.setShopQnAList(shopQnAList);

            return new AsyncResult<>(collectShopQnAListCallback);
        } catch (Exception e) {
            log.error("Failed collectQna", e);
            return null;
        }
    }

    @Async
    @Override
    public ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String token, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request) {
        return null;
    }

    @Override
    public ListenableFuture<ShopQnaJobDto.Request.PostCallback> postAnswerForQnaToShop(String token, long jobId, ShopQnaJobDto.Request.PostJob postJobDto) {
        log.info("Feelway postAnswerForQnaToShop Call");
        boolean successFlag = false;
        String resultMessage = null;
        try {

            String postAnswerForQnaPage = feelwayRequestPageService.postAnswerForQna(token, postJobDto);
            log.info("result html : {}", postAnswerForQnaPage);
            resultMessage = PatternExtractor.FEELWAY_POST_ANSWER_RESULT.extract(postAnswerForQnaPage,1);
            log.info("resultMessage : {}", resultMessage);
            successFlag = true;

            //답변 등록시에 해당 Question에 해당하는 목록을 보냄으로써 실제 등록된걸로 업데이트 처리한다.
            String collectResponse = feelwayRequestPageService.collectQna(token, ShopQnaJobDto.QuestionStatus.COMPLETE, postJobDto.getShopQna().getQuestionWriter());
            List<ShopQnaDto.Request.PostCallback> shopQnAList = new ArrayList<>(FeelwayQnAParser.parseQnaByQuestionId(collectResponse, postJobDto.getShopQna().getQuestionId()));
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

        } catch (IOException e) {
            log.error("Failed! post answer", e);
            resultMessage = "Failed! post answer";
            return null;
        }
    }

    /**
     * 판매 상품에 대한 삭제를 수행한다.
     * @param token
     * @param jobId
     * @param deleteSaleJob
     * @return
     */
    @Override
    public ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob) {
        log.info("Feelway deleteProductFromShop Call");
        boolean successFlag = false;
        String resultMessage = null;
        ShopSaleJobDto.SaleStatus saleStatus = ShopSaleJobDto.SaleStatus.DELETE; // 쇼핑몰로의 요청 자체가 실패 시에는 요청받은 status를 다시 돌려준다.
        try {
            String response = feelwayRequestPageService.deleteProduct(token, deleteSaleJob.getPostId());
            //결과값을 가지고 처리 결과 상태를 판단한다.
            resultMessage = PatternExtractor.FEELWAY_POST_ANSWER_RESULT.extract(response,1);
            log.info("delete Result:{}", resultMessage);
            if(resultMessage.contains("삭제되었습니다")){
                //삭제되었습니다
                successFlag = true;
            }
            log.info("delete resultMessage:{}",resultMessage);
            //해당 상품의 상태를 읽어 온다.
            FeelwaySellingProduct feelwaySellingProduct = getShopSaleStatudByProductNumber(token, deleteSaleJob.getPostId());
            if(feelwaySellingProduct != null){
                //삭제 실패상태
                saleStatus = feelwaySellingProduct.getSaleStatus();
                successFlag = false;
                resultMessage = "Real Delete Failed!!";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new AsyncResult<>(new RegisterDto.Response(
                deleteSaleJob.getId(), //shopSaleId
                deleteSaleJob.getPostId(),
                saleStatus,
                jobId,
                deleteSaleJob.getId(),
                successFlag,
                resultMessage
        ));
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.CollectCallback> collectOrderFromShop(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request) {
        log.info("Feelway collectOrderFromShop Call");
        boolean successFlag = false;
        String resultMessage = null;
        List<OrderDto.Request.CollectCallback> collectOrderList = null;
        try {
//            collectOrderList =  getOrderFromExcel(token);
            collectOrderList =  getOrderFromPage(token);
            successFlag = true;
            resultMessage = "수집성공";

            //TODO:주문중에 반송중(반품승인 버튼 클릭한 상태)일 경우 반품완료 처리를 하고 재수집을 한다.
            for(OrderDto.Request.CollectCallback collectOrder : collectOrderList){
                if(collectOrder.getStatus() == OrderDto.OrderStatus.RETURN_DELIVERY){
                    //반품완료 처리
                    feelwayRequestPageService.updateReturnComplete(
                            token,
                            collectOrder.getOrderId(),
                            collectOrder.getPostId(),
                            request.getLoginId(),
                            collectOrder.getBuyerId()
                    );
                    //해당주문만 재수집
                    List<OrderDto.Request.CollectCallback> collectOrderListById = getOrderFromPageByOrderId(token, collectOrder.getOrderId());
                    //실패로 인해서 제대로 변경이 안되는 경우 주문수집 실패처리
                    if(collectOrderListById.size() != 1 || collectOrderListById.get(0).getStatus() != OrderDto.OrderStatus.RETURN_COMPLETE){
                        //TODO:수집 데이타 오류 및 처리상태가 잘못 처리된 경우
                        log.error("Collect Order Fail!! - {}", collectOrderListById);
                        successFlag = false;
                        resultMessage = "반송중 처리로 인한 수집실패";
                        break;
                    }
                    //수집된 데이타로 변경처리
                    collectOrder = collectOrderListById.get(0);
                }
            }
        } catch (IOException e) {
            log.error("Failed! collectOrder", e);
            resultMessage = "Failed! collectOrder";
        }

        OrderJobDto.Request.CollectCallback collectOrderCallback = new OrderJobDto.Request.CollectCallback();
        collectOrderCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        collectOrderCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        collectOrderCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        collectOrderCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        collectOrderCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
        collectOrderCallback.setOrderList(collectOrderList);

        return new AsyncResult<>(collectOrderCallback);
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.UpdateCallback> updateOrderToShop(String token, long jobId, OrderJobDto.Request.UpdateJob updateJobDto) {
        boolean successFlag = false;
        String resultMessage = null;
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //1.현재 상태 수집
            collectOrderList = getOrderFromPageByOrderId(token, updateJobDto.getShopOrderId());
            if (collectOrderList.size() != 1) {
                //수집 데이타 오류
                log.error("Collect Order Fail!! - {}", collectOrderList);
                successFlag = false;
                resultMessage = "전송 전 수집 실패";
            }else {
                //현재 테이타 수집 성공
                OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);

                //2.상태값을 비교해서 처리가능,처리불필요,처리불가 상태를 판별한다.
                ComparisonCheckResult comparisonCheckResult = comparisonCheckRequestStatusWithShopStatus(updateJobDto.getStatus(), collectOrder.getStatus());
                switch(comparisonCheckResult){
                    case POSIBLE:
                        //3-1.작업가능상태
                        String response = feelwayRequestPageService.updateShopOrderByStatus(
                                token,
                                collectOrder.getOrderId(),
                                collectOrder.getPostId(),
                                collectOrder.getBuyerId(),
                                collectOrder.getStatus(),
                                updateJobDto
                        );
                        log.info("result html : {}", response);
                        resultMessage = PatternExtractor.FEELWAY_POST_ANSWER_RESULT.extract(response, 1);
                        log.info("resultMessage : {}", resultMessage);
                        successFlag = true;

                        //반품 승인 요청의 경우 - 반품승인버튼, 반품완료버튼 두가지 기능을 한다. 그래서 반품완료 추가 처리를 한다.
                        if(updateJobDto.getStatus() == OrderJobDto.Request.OrderUpdateActionStatus.RETURN_CONFIRM){
                            response = feelwayRequestPageService.updateReturnComplete(
                                    token,
                                    collectOrder.getOrderId(),
                                    collectOrder.getPostId(),
                                    updateJobDto.getShopAccount().getLoginId(),
                                    collectOrder.getBuyerId()
                            );
                            log.info("result html : {}", response);
                            resultMessage = PatternExtractor.FEELWAY_POST_ANSWER_RESULT.extract(response, 1);
                            log.info("resultMessage : {}", resultMessage);
                            successFlag = true;
                        }else if(updateJobDto.getStatus() == OrderJobDto.Request.OrderUpdateActionStatus.RETURN_REJECT){
                            //반품 거절 요청 시에 송장 업데이트 로직을 수행한다.
                            response = feelwayRequestPageService.createOrUpdateDeliveryOrder(
                                    token,
                                    collectOrder.getOrderId(),
                                    collectOrder.getPostId(),
                                    updateJobDto.getShopAccount().getLoginId(),
                                    collectOrder.getBuyerId(),
                                    updateJobDto.getSellerMessage(),
                                    updateJobDto.getCourier().getCode(),
                                    updateJobDto.getCourier().getCustomName(), // Code 값이 "ETC"인 경우에 사용되는 택배사명
                                    updateJobDto.getTrackingNumber(),
                                    "change_send_info"
                            );

                            log.info("result html : {}", response);
                            resultMessage = PatternExtractor.FEELWAY_POST_DELIVERY_UPDATE_ANSWER_RESULT.extract(response, 1);
                            log.info("resultMessage : {}", resultMessage);
                            successFlag = true;

                        }

                        break;
                    case NEEDLESS:
                        //3-2.작업불필요상태(이미 적용된 상태) - 작업 성공으로 기록하고 조회된 주문을 내려준다.
                        successFlag = true;
                        resultMessage = "상태변경 불필요";
                        break;
                    case IMPOSIBLE:
                        //3-3.작업불가상태(전혀 다른 상태값을 가져있는 경우) - 작업 실패로 기록하고 조회된 주문을 내려준다.
                        successFlag = false;
                        resultMessage = "상태변경 불가능";
                        break;
                }
            }
        }catch(Exception e){
            log.error("Failed! post answer", e);
            successFlag = false;
            resultMessage = "Failed! post updateOrderToShop";
        }

        //주문상태 업데이트가 성공하든 실패하든 쇼핑몰 주문상태 재수집
        try {
            collectOrderList = getOrderFromPageByOrderId(token, updateJobDto.getShopOrderId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //최종 response 구성
        OrderJobDto.Request.UpdateCallback updateCallback = new OrderJobDto.Request.UpdateCallback();
        updateCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        updateCallback.getJobTaskResponseBaseDto().setRequestId(updateJobDto.getShopAccount().getRequestId());
        updateCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        updateCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        updateCallback.setShopAccount(modelMapper.map(updateJobDto.getShopAccount(), ShopAccountDto.Response.class));
        updateCallback.setOrderList(collectOrderList);

        return new AsyncResult<>(updateCallback);
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.PostConversationCallback> postConversationMessageForOrderToShop(String token, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob) {

        return new AsyncResult<>(postOrderBaseConversationMessage(token, jobId, postConversationJob));
    }

    /**
     * 상품에 대해서 상태를 업데이트 한다. 판매중지 기능으로 사용된다.
     * @param token
     * @param jobId
     * @param updateSaleStatusJob
     * @return
     */
    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleStatusToShop(String token, long jobId, ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob) {
        String message = "";
        boolean successFlag = false;
        ShopSaleJobDto.SaleStatus saleStatus = updateSaleStatusJob.getRequestSaleStatus();
        try {
            boolean checkData;
            boolean isAbsence;
            if(updateSaleStatusJob.getRequestSaleStatus() == ShopSaleJobDto.SaleStatus.SALE_STOP){
                checkData = true;
                isAbsence = true;
            }else if(updateSaleStatusJob.getRequestSaleStatus() == ShopSaleJobDto.SaleStatus.ON_SALE){
                checkData = true;
                isAbsence = false;
            }else{
                checkData = false;
                isAbsence = false;
            }

            if(checkData){
                String result = feelwayRequestPageService.changeAbsenceProduct(token, updateSaleStatusJob.getPostId(), isAbsence);
                log.info("absence result - {}",result);

                //처리후에 현재 상품의 상태를 읽어온다.
                FeelwaySellingProduct feelwaySellingProduct = getShopSaleStatudByProductNumber(token, updateSaleStatusJob.getPostId());
                //판매글이 존재하지 않을경우
                if(ObjectUtils.isEmpty(feelwaySellingProduct)){
                    saleStatus = ShopSaleJobDto.SaleStatus.NOT_FOUND_SALE;
                    successFlag = false;
                    message = "삭제된 글";
                }else{
                    saleStatus = feelwaySellingProduct.getSaleStatus();
                    successFlag = true;
                    message = "변경성공";
                }

            }else{
                log.error("Status Not Found - Data Error");
                successFlag = false;
                message =  "Status Not Found - Request is " + updateSaleStatusJob.getRequestSaleStatus();
            }
        } catch (IOException e) {
            e.printStackTrace();//todo
            message = e.getMessage();
        }

        //실패시 요청받은 status를 다시 돌려준다.
        return new AsyncResult<>(new RegisterDto.Response(
                updateSaleStatusJob.getId(), //shopSaleId
                updateSaleStatusJob.getPostId(),
                saleStatus,
                jobId,
                updateSaleStatusJob.getId(),
                successFlag,
                message
        ));
    }

    public List<OrderDto.Request.CollectCallback> getOrderFromExcel(String token) throws IOException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        String jsonString = feelwayRequestPageService.requestOrderFromExcel(token, startDate, endDate);
        FeelwayOrderList feelwayOrderList = objectMapper.readValue(jsonString, FeelwayOrderList.class);

        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        for(FeelwayOrder feelwayOrder : feelwayOrderList.getData()){
            collectOrderList.add(modelMapper.map(feelwayOrder, OrderDto.Request.CollectCallback.class));
        }
        log.info("============================");
        log.info("{}", collectOrderList);

        return collectOrderList;
    }

    public List<OrderDto.Request.CollectCallback> getOrderFromPage(String token) throws IOException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        //최초페이지를 읽어온다.
        String htmlContents = feelwayRequestPageService.requestOrderFromPage(token, startDate, endDate, null);
        //1Page Data를 넣어준다.
        List<OrderDto.Request.CollectCallback> collectCallbackList = new ArrayList<>(FeelwayOrderParser.parseOrderList(htmlContents));

        //읽어온 1page를 기준으로 페이징을 확인한다.
        int pageCount = FeelwayOrderParser.getOrderListPaging(htmlContents);
        log.info("pageCount:{}",pageCount);
        for(int pageNumber = 2; pageNumber <= pageCount; pageNumber++){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<OrderDto.Request.CollectCallback> pageCollectCallbackList
                    = FeelwayOrderParser.parseOrderList(feelwayRequestPageService.requestOrderFromPage(token, startDate, endDate, String.valueOf(pageNumber)));
            log.info("Page-{}, pageCollectCallbackList:{}", pageNumber, pageCollectCallbackList);
            collectCallbackList.addAll(pageCollectCallbackList);
//            collectCallbackList.addAll(
//                    FeelwayOrderParser.parseOrderList(
//                            feelwayRequestPageService.requestOrderFromPage(token, startDate, endDate, String.valueOf(pageNumber))
//                    )
//            );
        }

        return collectCallbackList;
    }

    public List<OrderDto.Request.CollectCallback> getOrderFromPageByOrderId(String token, String shopOrderId) throws IOException {
        String htmlContents = feelwayRequestPageService.requestOrderFromPageByShopOrderId(token, "", "", shopOrderId, null);

        return FeelwayOrderParser.parseOrderList(htmlContents);
    }

    public OrderJobDto.Request.PostConversationCallback postOrderBaseConversationMessage(String token, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob) {
        log.info("Feelway postOrderBaseConversationMessage Call");
        boolean successFlag = false;
        String resultMessage = null;
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            String postConversationForOrder = feelwayRequestPageService.postConversationMessageForOrder(token, postConversationJob);
            log.info("result html : {}", postConversationForOrder);
            resultMessage = PatternExtractor.FEELWAY_POST_ANSWER_RESULT.extract(postConversationForOrder,1);
            log.info("resultMessage : {}", resultMessage);
            successFlag = true;

            collectOrderList = getOrderFromPageByOrderId(token, postConversationJob.getShopOrderId());
            if(collectOrderList.size() != 1){
                //TODO:수집 데이타 오류
                successFlag = false;
                resultMessage = "전송 후 수집 실패";
            }
        } catch (IOException e) {
            log.error("Failed! post answer", e);
            resultMessage = "Failed! post answer";
        }

        //최종 response 구성
        OrderJobDto.Request.PostConversationCallback postConversationCallback = new OrderJobDto.Request.PostConversationCallback();
        postConversationCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        postConversationCallback.getJobTaskResponseBaseDto().setRequestId(postConversationJob.getShopAccount().getRequestId());
        postConversationCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        postConversationCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        postConversationCallback.setShopAccount(modelMapper.map(postConversationJob.getShopAccount(), ShopAccountDto.Response.class));
        postConversationCallback.setOrderBaseConversationList(collectOrderList.get(0).getOrderBaseConversationList());

        return postConversationCallback;
    }

    public String updateOrderToSellCancel(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateCancelOrder");
        List<OrderDto.Request.CollectCallback> collectOrderList = getOrderFromPageByOrderId(token, updateJob.getShopOrderId());
        if(collectOrderList.size() != 1){
            //TODO:수집 데이타 오류
            log.error("Collect Order Fail!! - {}", collectOrderList);
        }
        OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);
        String response = feelwayRequestPageService.updateCancelOrder(
                token,
                collectOrder.getOrderId(),
                collectOrder.getPostId(),
                updateJob.getShopAccount().getLoginId(),
                collectOrder.getBuyerId(),
                updateJob.getSellerMessage()
        );
        log.info("response:{}",response);
        //response를 파싱하여 성공여부를 판단하고 진행한다.
        return response;
    }

    /**
     * 반품동의에 대한 처리를 수행한다.
     * @param token
     * @param jobId
     * @param updateJob
     * @return
     * @throws IOException
     */
    //TODO:테스트 필요
    public String updateReturnConfirm(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob, String mode) throws IOException {
        log.info("Call updateReturnConfirm");
        List<OrderDto.Request.CollectCallback> collectOrderList = getOrderFromPageByOrderId(token, updateJob.getShopOrderId());
        if(collectOrderList.size() != 1){
            //TODO:수집 데이타 오류
            log.error("Collect Order Fail!! - {}", collectOrderList);
        }
        OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);
        String response = feelwayRequestPageService.updateReturnConfirm(
                token,
                collectOrder.getOrderId(),
                collectOrder.getPostId(),
                updateJob.getShopAccount().getLoginId(),
                collectOrder.getBuyerId(),
                updateJob.getSellerMessage()
        );
        log.info("response:{}", response);
        //response를 파싱하여 성공여부를 판단하고 진행한다.
        return response;
    }

    /**
     * 반품거절에 대한 처리를 수행한다.
     *
     * @param token
     * @param jobId
     * @param updateJob
     * @return
     * @throws IOException
     */
    //TODO:테스트 필요
    public String updateReturnReject(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob, String mode) throws IOException {
        log.info("Call updateReturnReject");
        List<OrderDto.Request.CollectCallback> collectOrderList = getOrderFromPageByOrderId(token, updateJob.getShopOrderId());
        if(collectOrderList.size() != 1){
            //TODO:수집 데이타 오류
            log.error("Collect Order Fail!! - {}", collectOrderList);
        }

        log.info("{}",collectOrderList.get(0).getOrderId());
        log.info("{}",collectOrderList.get(0).getStatus());
        OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);
        String response = feelwayRequestPageService.updateReturnReject(
                token,
                collectOrder.getOrderId(),
                collectOrder.getPostId(),
                updateJob.getShopAccount().getLoginId(),
                collectOrder.getBuyerId(),
                updateJob.getSellerMessage()
        );
        log.info("response:{}", response);
        //response를 파싱하여 성공여부를 판단하고 진행한다.
        return response;
    }

    /**
     * 반품완료에 대한 처리를 수행한다.
     *
     * @param token
     * @param jobId
     * @param updateJob
     * @return
     * @throws IOException
     */
    //TODO:테스트 필요
    public String updateReturnComplete(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob, String mode) throws IOException {
        log.info("Call updateReturnReject");
        List<OrderDto.Request.CollectCallback> collectOrderList = getOrderFromPageByOrderId(token, updateJob.getShopOrderId());
        if(collectOrderList.size() != 1){
            //TODO:수집 데이타 오류
            log.error("Collect Order Fail!! - {}", collectOrderList);
        }

        log.info("{}",collectOrderList.get(0).getOrderId());
        log.info("{}",collectOrderList.get(0).getStatus());
        OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);
        String response = feelwayRequestPageService.updateReturnComplete(
                token,
                collectOrder.getOrderId(),
                collectOrder.getPostId(),
                updateJob.getShopAccount().getLoginId(),
                collectOrder.getBuyerId()
        );
        log.info("response:{}",response);
        //response를 파싱하여 성공여부를 판단하고 진행한다.
        return response;
    }

    /**
     *
     * @param token
     * @param jobId
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateOrderToDelivery(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob, String mode) throws IOException {
        log.info("Call updateDeliveryOrder");
        List<OrderDto.Request.CollectCallback> collectOrderList = getOrderFromPageByOrderId(token, updateJob.getShopOrderId());
        if(collectOrderList.size() != 1){
            //TODO:수집 데이타 오류
            log.error("Collect Order Fail!! - {}", collectOrderList);
        }
        OrderDto.Request.CollectCallback collectOrder = collectOrderList.get(0);
        log.info("{}",collectOrder);

        log.info("updateJob:{}",updateJob);
        String response = feelwayRequestPageService.createOrUpdateDeliveryOrder(
                token,
                collectOrder.getOrderId(),
                collectOrder.getPostId(),
                updateJob.getShopAccount().getLoginId(),
                collectOrder.getBuyerId(),
                updateJob.getSellerMessage(),
                updateJob.getCourier().getCode(),
                updateJob.getCourier().getCustomName(), // Code 값이 "ETC"인 경우에 사용되는 택배사명
                updateJob.getTrackingNumber(),
                mode
        );
        log.info("response:{}",response);
        //response를 파싱하여 성공여부를 판단하고 진행한다.
        return response;
    }

    /**
     * 배송정보 업데이트의 경우 mode만 변경해서 배송입력 프로세스와 동일하다
     * @param token
     * @param jobId
     * @param updateJob
     * @return
     * @throws IOException
     */
    public String updateDeliveryInfo(String token, long jobId, OrderJobDto.Request.UpdateJob updateJob) throws IOException {
        log.info("Call updateDeliveryInfo");
        return updateOrderToDelivery(token, jobId, updateJob, "change_send_info");
    }


    private void setDtoToFeelwayProductForUpdate(FeelwayProductForUpdate feelwayProduct, OnlineSaleDto onlineSaleDto) {
        setDtoToFeelwayProduct(feelwayProduct, onlineSaleDto);
        feelwayProduct.setMode(FeelwayProduct.Mode.UPDATE.getModeName());
    }

    private void setDtoToFeelwayProductForCreate(FeelwayProductForCreate feelwayProduct, OnlineSaleDto onlineSaleDto) {
        setDtoToFeelwayProduct(feelwayProduct, onlineSaleDto);

        OnlineSaleFeelwayDto onlineSaleFeelway = onlineSaleDto.getSaleFeelway();
        feelwayProduct.setPowerPeriod(onlineSaleFeelway.getPowerSalePeriod());
    }

    private void setDtoToFeelwayProduct(FeelwayProduct feelwayProduct, OnlineSaleDto onlineSaleDto) {
        // 판매 수정 및 생성에서 쓰이는 공통 데이터 세팅
        OnlineSaleFeelwayDto onlineSaleFeelway = onlineSaleDto.getSaleFeelway();
        feelwayProduct.setGoodsPrice(onlineSaleDto.getPrice());
        feelwayProduct.setBrandId(onlineSaleDto.getBrandMap().getSourceCode());
        if (onlineSaleDto.getBrandMap().getDirectFlag()) {
            feelwayProduct.setOtherBrandName(onlineSaleDto.getBrandMap().getSourceName());
        }
        feelwayProduct.setNewProduct(onlineSaleDto.getCondition());
        feelwayProduct.setCategoryId(onlineSaleDto.getShopSale().getShopCategory().getShopCategoryCode());
        feelwayProduct.setSubCategoryId(onlineSaleDto.getShopSale().getShopCategory().getChild().getShopCategoryCode());
        feelwayProduct.setGoodsName(onlineSaleDto.getSubject());
        feelwayProduct.setGoodsOrigin(onlineSaleDto.getProductionCountry());
        List<ProductOptionDto> productOptionDtoList = onlineSaleDto.getProductList().stream()
                .flatMap(product -> product.getProductOptionList().stream())
                .collect(Collectors.toList());
        if (productOptionDtoList.size() == 1) {
            feelwayProduct.setGoodsSize(productOptionDtoList.get(0).getName());
        } else {
            feelwayProduct.setGoodsSize("[상세 설명 참조]");
        }
        feelwayProduct.setGoodsBuyYear(onlineSaleFeelway.getBuyYear());
        feelwayProduct.setGoodsBuyPlace(onlineSaleFeelway.getBuyPlace());
        feelwayProduct.setMaterial(onlineSaleDto.getMaterial());
        feelwayProduct.setColor(onlineSaleDto.getColor());
        feelwayProduct.setCompany(onlineSaleDto.getProductionCompany());
        feelwayProduct.setNotice(onlineSaleDto.getPrecaution());
        feelwayProduct.setStandardGuarantee(onlineSaleDto.getQualityAssuranceStandards());
        feelwayProduct.setCustomerServiceManagerPhone(onlineSaleDto.getCsStaffPhone());
        feelwayProduct.setDatePeriod(onlineSaleDto.getProductionDate());
        feelwayProduct.setGoodsScratch(onlineSaleFeelway.getDamage());
        feelwayProduct.setPart1(onlineSaleDto.isWarrantyExistenceFlag());
        feelwayProduct.setPart2(onlineSaleFeelway.isTagProvisionFlag());
        feelwayProduct.setPart3(onlineSaleFeelway.isGuaranteeCardProvisionFlag());
        feelwayProduct.setPart4(onlineSaleFeelway.isDustBagProvisionFlag());
        feelwayProduct.setPart5(onlineSaleFeelway.isCaseProvisionFlag());
        feelwayProduct.setPartText(onlineSaleFeelway.getOtherItem());
        feelwayProduct.setCard(onlineSaleFeelway.isCardAvailabilityFlag());
        feelwayProduct.setGiftFeelwayCoupon(Integer.toString(onlineSaleFeelway.getGiftFeelponCount()));
        feelwayProduct.setSendingNation(onlineSaleFeelway.getDeliveryType().getOriginValue());
        feelwayProduct.setSendingPayer(onlineSaleFeelway.getDeliveryPayer().getOriginValue());
        feelwayProduct.setSendingMethod(onlineSaleFeelway.getDeliveryMethod().getOriginValue());
        feelwayProduct.setSendingPrice(onlineSaleFeelway.getExpectedDeliveryFee());
        feelwayProduct.setSendingPeriod(onlineSaleFeelway.getExpectedDeliveryPeriod());
        feelwayProduct.setPhone(onlineSaleDto.getCsStaffPhone());
        feelwayProduct.setMobilePhone(onlineSaleDto.getCsStaffPhone());
        feelwayProduct.setGoodsIntro(onlineSaleDto.getDetail());

        Integer maxQuantity = onlineSaleDto.getProductList().stream()
                .flatMap((product) -> product.getProductOptionList().stream()
                        .map(ProductOptionDto::getQuantity)
                ).max(Integer::compare).get();

        feelwayProduct.setStockTotalAmount(maxQuantity.toString());

        feelwayProduct.setRealPhoto(onlineSaleDto.getDirectPictureFlag());
    }

    public List<CategoryDto> getCategoryList(String token) {
        try {
            String registerProductPage =
                    feelwayRequestPageService.requestRegisterProductPage(token);
            return FeelwayProductParser.getCategories(registerProductPage);
        } catch (IOException e) {
// todo 해당 에러는 job 테이블의 상태 데이터를 변경해야 한다.
            e.printStackTrace();
        }
        return null;
    }

    public ShopQnaJobDto.Request.CollectCallback collectQna(String token, ShopQnaJobDto.QuestionStatus questionStatus, String askId)
            throws IOException {
        log.info("Call collectQna");
        String response = feelwayRequestPageService.collectQna(token, questionStatus, askId);
        List<ShopQnaDto.Request.CollectCallback> shopQnAList = new ArrayList<>(FeelwayQnAParser.parseQna(response));
        shopQnAList.sort((p1, p2) -> p2.getQuestionId().compareTo(p1.getQuestionId()));
        ShopQnaJobDto.Request.CollectCallback collectShopQnAListCallback = new ShopQnaJobDto.Request.CollectCallback();
        collectShopQnAListCallback.setShopQnAList(shopQnAList);

        return collectShopQnAListCallback;
    }

    /**
     * 요청 들어온 상태 기준으로 작업 필요,불필요,불가능 상태를 판단한다.
     *
     * @param requestStatus
     * @param shopCurrentStatus
     * @return
     */
    private ComparisonCheckResult comparisonCheckRequestStatusWithShopStatus(OrderJobDto.Request.OrderUpdateActionStatus requestStatus,
                                                               OrderDto.OrderStatus shopCurrentStatus){
        switch(requestStatus){
            case EXCHANGE_CONFIRM: //필웨이에는 존재하지 않는 기능임
            case EXCHANGE_REJECT: //필웨이에는 존재하지 않는 기능임
            case CALCULATION_DELAY: //필웨이에는 존재하지 않는 기능임
                return ComparisonCheckResult.IMPOSIBLE;
            case DELIVERY_READY: //필웨이에는 존재하지 않지만 ByPass로 결과를 내려줘야 함
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //결제확인됨,입금확인(PAYMENT_COMPLETE)
                        return ComparisonCheckResult.NEEDLESS;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case DELIVERY:
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //결제확인됨,입금확인(PAYMENT_COMPLETE)
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청(배송전 구매취소 BUY_CANCEL_REQUEST)
                    case DELIVERY: //배송중(DELIVERY)
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case BUY_CANCEL_CONFIRM:
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태일 경우는 이미 처리된 상태로 간주
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case BUY_CANCEL_REJECT:
                switch(shopCurrentStatus){
                    case DELIVERY: //배송중 상태일 경우는 이미 처리된 상태로 간주
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case SELL_CANCEL:
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case PAYMENT_COMPLETE: //결제확인됨,입금확인
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST)
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_CONFIRM:
                switch(shopCurrentStatus){
                    case RETURN_COMPLETE: //반품완료 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST)
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_REJECT:
                switch(shopCurrentStatus){
                    case DELIVERY: //배송중 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST)
                    case RETURN_REJECT: // 반품거절 상태는 이미 반품거절된 상태
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case CALCULATION_SCHEDULE:
                switch(shopCurrentStatus){
                    case CALCULATION_SCHEDULE: //정산요청중
                    case CALCULATION_COMPLETE: //정산완료
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CONFIRM: //구매결정확인(구매결정 BUY_CONFIRM)
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            default:
                log.error("Not Matched Shop Status - ShopStatus:{}, RequestStatus:{}",shopCurrentStatus.name(), requestStatus.name());
                return ComparisonCheckResult.IMPOSIBLE;
        }
    }

    /**
     * 현재의 쇼핑몰 상태에 대해서 요청들어온 상태와 비교하여 작업 여부를 결정한다.
     * @param requestStatus
     * @param shopCurrentStatus
     * @return
     */
    private boolean isExistFunctionEachOrderStatus(OrderJobDto.Request.OrderUpdateActionStatus requestStatus,
                                                               OrderDto.OrderStatus shopCurrentStatus){
        switch(shopCurrentStatus){
            case PAYMENT_NOT_CONFIRM: //입금미확인(PAYMENT_NOT_CONFIRM) - 현재 처리하지 않는 상태
            case RETURN_REJECT: //반품거절(RETURN_REJECT) - 요청상태는 존재하지만 실제 상태는 배송중임
            case CALCULATION_SCHEDULE: //정산요청중(정산요청 CALCULATION_REQUEST)
            case CALCULATION_COMPLETE: //정산완료(CALCULATION_COMPLETE)
            case SELL_CANCEL: //판매취소(SELL_CANCEL) - 처리할 수 있는 상태가 없음
            case RETURN_COMPLETE: //반품완료(RETURN_COMPLETE)
                return false;

            case PAYMENT_COMPLETE: //결제확인됨,입금확인(PAYMENT_COMPLETE)
            case BUY_CANCEL_REQUEST: //배송전 구매취소 요청(배송전 구매취소 BUY_CANCEL_REQUEST)
                switch(requestStatus){
                    case DELIVERY: //배송확인
                    case SELL_CANCEL: //판매취소
                    case BUY_CANCEL_CONFIRM: //구매 취소 승인
                    case BUY_CANCEL_REJECT: //구매 취소 거절
                        return true;
                    default:
                        return false;
                }

            case DELIVERY: //배송중(DELIVERY)
                switch(requestStatus){
                    case DELIVERY: //배송중(송장업데이트)
                        return true;
                    default:
                        return false;
                }

            case BUY_CONFIRM: //구매결정확인(구매결정 BUY_CONFIRM)
                switch(requestStatus){
                    case CALCULATION_SCHEDULE: //정산요청
                        return true;
                    default:
                        return false;
                }

            case RETURN_REQUEST: //반품요청(RETURN_REQUEST)
                switch(requestStatus){
                    case RETURN_CONFIRM: //반품성사, 반품동의
                    case RETURN_REJECT: //반품거절
//                    case RETURN_DELIVERY: //반송중
                    case SELL_CANCEL: //판매취소
                        return true;
                    default:
                        return false;
                }

            default:
                log.error("Not Matched Shop Status - ShopStatus:{}, RequestStatus:{}",shopCurrentStatus.name(), requestStatus.name());
                return false;
        }
    }


    /**
     * 상품번호를 가지고 목록에서 정보를 조회한다.
     * 판매중탭과 판매완료 탭 두군데서 조회해서 결과를 가져온다.
     * @param token
     * @param productNumber
     * @return
     */
    public FeelwaySellingProduct getShopSaleStatudByProductNumber(String token, String productNumber) throws IOException {
        //판매중 탭의 물건 체크
        String sellingHtml =  feelwayRequestPageService.getProducyByProductNumberAndStatus(token, productNumber,false);
        FeelwaySellingProduct feelwaySellingProduct = FeelwayProductParser.getlatestSellingProduct(sellingHtml);
        //상품이 없거나 상태가 조회 불가일 경우, 판매완료 탭에서 조회한다.
        if(ObjectUtils.isEmpty(feelwaySellingProduct)){
            log.info("Not Found product in Selling Tab");
            //차단 방지를 위한 쿨타임
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //판매완료 탭의 물건 체크
            String notSellingHtml =  feelwayRequestPageService.getProducyByProductNumberAndStatus(token, productNumber,true);
            FeelwaySellingProduct feelwayNotSellingProduct = FeelwayProductParser.getlatestSellingProduct(notSellingHtml);
            //두번째 탭 확인 결과가 없을 경우 null을 리턴한다.
            if(ObjectUtils.isEmpty(feelwayNotSellingProduct)){
                log.info("Not Found product in Not-Selling Tab - productNumber:{}", productNumber);
                return null;
            }
            //완료탭에 물건이 있는 경우
            return feelwayNotSellingProduct;
        }else{
            //판매중 탭에 데이타 결과가 나온경우
            return feelwaySellingProduct;
        }
    }



}
