package com.wanpan.app.service.gorda;

import com.wanpan.app.config.gateway.GordaSellerClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.godra.GordaSellerTokenDto;
import com.wanpan.app.dto.godra.seller.delivery.DeliveryDto;
import com.wanpan.app.dto.godra.seller.my.SellerInfoResponse;
import com.wanpan.app.dto.godra.seller.order.CancelreturnDto;
import com.wanpan.app.dto.godra.seller.order.GordaItemRequest;
import com.wanpan.app.dto.godra.seller.order.GordaItemResponse;
import com.wanpan.app.dto.godra.type.CancelreturnReasonType;
import com.wanpan.app.dto.godra.type.CancelreturnType;
import com.wanpan.app.dto.godra.type.OrderItemStatus;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.dto.job.order.CourierDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.ShopAccountToken;
import com.wanpan.app.exception.InvalidRequestException;
import com.wanpan.app.service.ComparisonCheckResult;
import com.wanpan.app.service.ShopService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class GordaService  implements ShopService {
    private final GordaSellerClient gordaSellerClient;
    private GordaBrandService gordaBrandService;
    private final ModelMapper modelMapper;

    @Override
    public ShopAccountDto.Response checkSignIn(String loginId, String password, ShopAccountDto.Response shopAccountResponseDto) throws IOException {
        shopAccountResponseDto.setSuccessFlag(false);
        try {
            String token = getToken(loginId, password, ShopAccountToken.Type.SESSION);

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
            SellerInfoResponse body = getMyInfo(token);

            return body != null;
        } catch (Exception e) {
            // todo
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getToken(String accountId, String password, ShopAccountToken.Type tokenType) {
        try {
            return getTokenByApi(new GordaSellerTokenDto.Request(accountId, password));

        } catch (Exception e) {
            // todo
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<BrandDto> getBrandList(String token) {
        return gordaBrandService.getBrandList();
    }

    @Override
    public List<CategoryDto> getCategoryList(String token) {
        return null;
    }

    /**
     * 상품매핑, 상품옵션매핑, 상품등록 에 대한 세가지 액션을 모두 처리한다.
     * @param token
     * @param job
     * @param onlinesaleDto
     * @return
     */
    @Override
    public ListenableFuture<RegisterDto.Response> postSaleToShop(String token, Job job, OnlineSaleDto onlinesaleDto) {
        //TODO:상품에 대한 고르다 ProductId를 확인한다.(없으면 매핑요청, 있으면 등록요청)
        if(StringUtils.isEmpty(onlinesaleDto.getShopSale().getShopProductId())){
            //TODO:매핑요청 관련 로직수행
        }else{
            //TODO:등록요청 관련 로직수행
        }

        return null;
    }







    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleToShop(String token, long jobId, OnlineSaleDto onlineSaleDto) throws IOException {
        return null;
    }

    @Override
    public ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String token, long jobId, ShopQnaJobDto.QuestionStatus questionStatus, ShopAccountDto.Request request) {
        return null;
    }

    @Override
    public ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String token, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request) {
        return null;
    }

    @Override
    public ListenableFuture<ShopQnaJobDto.Request.PostCallback> postAnswerForQnaToShop(String token, long jobId, ShopQnaJobDto.Request.PostJob postJobDto) {
        return null;
    }

    @Override
    public ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob postJobDto) {
        return null;
    }

    @Override
    public ListenableFuture<OrderJobDto.Request.CollectCallback> collectOrderFromShop(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request) {
        log.info("Gorda collectOrderFromShop Call");
        log.info("token: {}",token);
        boolean successFlag = false;
        String resultMessage = "";
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //주문목록을 얻어온다. 목록조회 시작일이 없을 경우는 전체 스캔을 요청한다.
            List<GordaItemResponse> orderItemList = getMyOrders(token, request.getLatestCollectOrderAt());
            //얻어온 주문목록을 파싱 및 매핑한다.
            collectOrderList = convertGordaOrderItemsToCollectCallback(orderItemList);
        }catch(Exception e){
            log.error("collectOrder Exception Fail",e);
            successFlag = false;
            resultMessage = "Exception";
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

    public OrderJobDto.Request.CollectCallback collectOrderFromShopTest(String token, long jobId, ShopAccountDto.Request request) {
        log.info("Gorda collectOrderFromShopTest Call");
        log.info("token: {}",token);
        boolean successFlag = false;
        String resultMessage = "";
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //주문목록을 얻어온다. 목록조회 시작일이 없을 경우는 전체 스캔을 요청한다.
            List<GordaItemResponse> orderItemList = getMyOrders(token, request.getLatestCollectOrderAt());
            //얻어온 주문목록을 파싱 및 매핑한다.
            collectOrderList = convertGordaOrderItemsToCollectCallback(orderItemList);
        }catch(Exception e){
            log.error("collectOrder Exception Fail",e);
            successFlag = false;
            resultMessage = "Exception";
        }

        OrderJobDto.Request.CollectCallback collectOrderCallback = new OrderJobDto.Request.CollectCallback();
        collectOrderCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        collectOrderCallback.getJobTaskResponseBaseDto().setRequestId(request.getRequestId());
        collectOrderCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        collectOrderCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        collectOrderCallback.setShopAccount(modelMapper.map(request, ShopAccountDto.Response.class));
        collectOrderCallback.setOrderList(collectOrderList);

        return collectOrderCallback;
    }

    /**
     * 요청 들어온 상태 기준으로 작업 필요,불필요,불가능 상태를 판단한다.
     * @param requestStatus
     * @param shopCurrentStatus
     * @return
     */
    private ComparisonCheckResult comparisonCheckRequestStatusWithShopStatus(OrderJobDto.Request.OrderUpdateActionStatus requestStatus,
                                                                             OrderDto.OrderStatus shopCurrentStatus){
        switch(requestStatus){
            case DELIVERY_READY: //배송 준비중 변경 요청(발주확인 버튼)
                switch(shopCurrentStatus){ //고르다 결제완료 상태에서만 발주확인 가능
                    case PAYMENT_COMPLETE: //주문완료(PAYMENT_COMPLETE), 결제완료
                        return ComparisonCheckResult.POSIBLE;
                    case DELIVERY_READY: //같은 상태일때는 작업이 필요 없음
                        return ComparisonCheckResult.NEEDLESS;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case DELIVERY:
                switch(shopCurrentStatus){
                    case PAYMENT_COMPLETE: //주문완료(PAYMENT_COMPLETE)-두단계 처리를 해줘야함(주문완료->배송준비중->배송중)->송장입력(delivery쪽 API이용해서 배송중 자동 변경)
                    case DELIVERY_READY: //배송준비중 -> 송장입력(delivery쪽 API이용해서 배송중 자동 변경)
                    case DELIVERY: //배송중(DELIVERY) - 송장업데이트 기능
                        return ComparisonCheckResult.POSIBLE;
                    default: //BUY_CANCEL_REQUEST: //배송전 구매취소 요청
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case BUY_CANCEL_CONFIRM: //결제취소완료(PAYMENT_CANCELLATION)와 동일하다 봄
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태일 경우는 이미 처리된 상태로 간주
                        return ComparisonCheckResult.NEEDLESS;
                    case BUY_CANCEL_REQUEST: //배송전 구매취소 요청 - 취소 처리 버튼 기능
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case SELL_CANCEL: //판매취소 버튼 처리
                switch(shopCurrentStatus){
                    case SELL_CANCEL: //판매취소 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case PAYMENT_COMPLETE: //주문완료 - 판매취소 버튼 기능(판매취소 사유(코드선택),판매취소 상세사유 입력)
                    case DELIVERY_READY: //구매자 취소 요청 상태일때 - 취소처리 버튼 기능
                    case BUY_CANCEL_REQUEST: //구매자 취소 요청 상태일때 - 취소처리 버튼 기능
                        return ComparisonCheckResult.POSIBLE;
                    default: //RETURN_REQUEST: 반품요청일 경우 리본즈는 처리불가
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_CONFIRM:
                switch(shopCurrentStatus){
                    case RETURN_COMPLETE: //반품완료 상태는 이미 처리되어 있는 상태
                        return ComparisonCheckResult.NEEDLESS;
                    case RETURN_REQUEST: //반품요청(RETURN_REQUEST) - 반품확인부터 반품완료까지 동시 처리
                    case RETURN_CONFIRM: //반품승인(RETURN_CONFIRM,RETURN_PRODUCT_PICK_UP,RETURN_PRODUCT_PICK_UP_COMPLETED,RETURN_PRODUCT_CHECK) - 다음 스탭부터 반품완료까지 동시 처리
                        return ComparisonCheckResult.POSIBLE;
                    default:
                        return ComparisonCheckResult.IMPOSIBLE;
                }
            case RETURN_REJECT: //반품거절 - 고르다에서 지원하지 않는 기능임. 개발예정
            case EXCHANGE_CONFIRM: //필웨이,리본즈에는 존재하지 않는 기능임. 고르다 개발예정
            case EXCHANGE_REJECT: //필웨이,리본즈에는 존재하지 않는 기능임. 고르다 개발예정
            case BUY_CANCEL_REJECT: //구매취소거절 - 리본즈에는 존재하지 않는 기능임 - 거절시 고객센타 이용 메세지
            case CALCULATION_DELAY: //정산보류 - 필웨이,리본즈에는 존재하지 않는 기능임. 고르다 개발예정
            case CALCULATION_SCHEDULE: //정산요청 - 리본즈에서 정산은 자동으로 이루어 지기 때문에 처리할 사항이 없음. 고르다 개발예정
                return ComparisonCheckResult.IMPOSIBLE;
            default:
                log.error("Not Matched Shop Status - ShopStatus:{}, RequestStatus:{}",shopCurrentStatus.name(), requestStatus.name());
                return ComparisonCheckResult.IMPOSIBLE;
        }
    }

    /**
     * 주문상태에 대해 변경 과정을 진행한다.
     * @param token
     * @param jobId
     * @param updateJobDto
     * @return
     */
    @Override
    public ListenableFuture<OrderJobDto.Request.UpdateCallback> updateOrderToShop(String token, long jobId, OrderJobDto.Request.UpdateJob updateJobDto) {
        log.info("Gorda updateOrderToShop JOB START => jobId={}", jobId);
        boolean successFlag = false;
        String resultMessage = null;
        List<OrderDto.Request.CollectCallback> collectOrderList = new ArrayList<>();
        try {
            //1.현재 상태 수집
            OrderDto.Request.CollectCallback collectOrder = getOrderById(token, updateJobDto.getShopOrderId());
            if (collectOrder == null) {
                //수집 데이타 오류
                log.error("Gorda updateOrderToShop - (Before update) Collect Order Fail!! - {}", collectOrder);
                successFlag = false;
                resultMessage = "(업데이트 전) 주문 수집 실패";
            }else {
                //현재 테이타 수집 성공 Case
                //2.상태값을 비교해서 처리가능,처리불필요,처리불가 상태를 판별한다.
                ComparisonCheckResult comparisonCheckResult = comparisonCheckRequestStatusWithShopStatus(updateJobDto.getStatus(), collectOrder.getStatus());
                switch(comparisonCheckResult){
                    case POSIBLE: {
                        //3-1.작업가능상태
                        //해야 할 작업을 선택한다.
                        Object response = null;
                        String requestMessage = "";
                        switch (updateJobDto.getStatus()) {

                            case DELIVERY_READY: //배송 준비중(주문 확인 버튼) 처리 - 상태만 변경하면됨
                            {
                                requestMessage = "주문확인 요청";
                                response = updateOrderStatusByGordaStatus(token, updateJobDto.getShopOrderId(), OrderItemStatus.PRODUCT_PREPARATION);
                                break;
                            }

                            case DELIVERY: //배송중 송장저장 기능(주문완료 상태일때도 배송중 한번으로 처리 가능)
                            {
                                requestMessage = "배송중 요청";
                                if (collectOrder.getStatus() == OrderDto.OrderStatus.PAYMENT_COMPLETE
                                        || collectOrder.getStatus() == OrderDto.OrderStatus.DELIVERY_READY) {
                                    //주문완료 - 송장입력시 자동으로 배송중(송장입력)상태로 변경됨
                                    //배송준비중(발주확인) - 송장입력시 자동으로 배송중(송장입력)상태로 변경됨
                                    response = createDeliveryInfo(
                                            token, updateJobDto.getShopOrderId(), updateJobDto.getCourier(), updateJobDto.getTrackingNumber());
                                } else if (collectOrder.getStatus() == OrderDto.OrderStatus.DELIVERY) {
                                    //배송중 - 송장수정 기능
                                    response = updateDeliveryInfo(
                                            token, collectOrder.getShopDeliveryId(), updateJobDto.getCourier(), updateJobDto.getTrackingNumber());
                                } else {
                                    log.error("Failed DELIVERY process");
                                }
                                break;
                            }

                            case BUY_CANCEL_CONFIRM: //취소 처리 버튼 기능 - 결제취소완료 상태가 됨
                            {
                                requestMessage = "구매취소 요청";
                                response = updateOrderStatusByGordaStatus(token, updateJobDto.getShopOrderId(), OrderItemStatus.PAYMENT_CANCELLATION);
                                break;
                            }

                            case SELL_CANCEL: //판매취소버튼, 취소처리버튼 - 결제취소완료 상태가 됨
                            {
                                requestMessage = "판매취소 요청";
                                String detailReason = "불편을 드려 대단히 죄송합니다. 부득이한 사정으로 인해 판매취소를 진행하였습니다.";
                                if (collectOrder.getStatus() == OrderDto.OrderStatus.PAYMENT_COMPLETE
                                    || collectOrder.getStatus() == OrderDto.OrderStatus.DELIVERY_READY) {
                                    //PAYMENT_COMPLETE, DELIVERY_READY: 발주확인, 주문완료상태일때 - 판매취소 버튼 기능(판매취소 사유(코드선택),판매취소 상세사유 입력) - 결제취소완료가됨
                                    //ID존재 여부에 따라서
                                    if(collectOrder.getShopCancelreturnId() > 0){
                                        response = updateCancelreturn(token, collectOrder.getShopCancelreturnId(), CancelreturnReasonType.CANCEL_ETC,detailReason);

                                    }else{
                                        response = createCancelreturn(token, updateJobDto.getShopOrderId(), CancelreturnReasonType.CANCEL_ETC,detailReason);
                                    }
                                }else if(collectOrder.getStatus() == OrderDto.OrderStatus.BUY_CANCEL_REQUEST){
                                    //BUY_CANCEL_REQUEST: 구매자 취소 요청 상태일때 - 취소처리 버튼 기능 - 결제취소완료가됨
                                    response = updateOrderStatusByGordaStatus(token, updateJobDto.getShopOrderId(), OrderItemStatus.PAYMENT_CANCELLATION);
                                } else {
                                    log.error("Failed SELL_CANCEL process");
                                }
                                break;
                            }

                            case RETURN_CONFIRM: //요청상태 반품승인 -> 반품완료처리 상태의 경우 쇼핑몰 자체의 상태에 따라 가운데 추가되는 step가 발생한다.
                            {
                                //API에서는 중간단계가 없이 바로 반품완료로 호출한다.
                                requestMessage = "반품(승인)완료 요청";
                                response = updateOrderStatusByGordaStatus(token, updateJobDto.getShopOrderId(), OrderItemStatus.RETURN_COMPLETED);
                                break;
                            } //RETURN_CONFIRM
                        }
                        log.info("result html : {}", response);
                        //결과 json에 대한 처리 - 형식이 다 달라서 null체크로만 처리함
                        if(response != null){
                            successFlag = true;
                            resultMessage = requestMessage + " 성공";
                        }else{
                            successFlag = false;
                            resultMessage = requestMessage + " 실패";
                        }

                        log.info("resultMessage : {}", resultMessage);

                        //POST 이후 다시 수집을 해서 return을 구성한다.
                        collectOrder = getOrderById(token, updateJobDto.getShopOrderId());
                        break;
                    } //POSIBLE

                    case NEEDLESS: {
                        //3-2.작업불필요상태(이미 적용된 상태) - 작업 성공으로 기록하고 조회된 주문을 내려준다.
                        successFlag = true;
                        resultMessage = "상태변경 불필요";
                        break;
                    }
                    case IMPOSIBLE: {
                        //3-3.작업불가상태(전혀 다른 상태값을 가져있는 경우) - 작업 실패로 기록하고 조회된 주문을 내려준다.
                        successFlag = false;
                        resultMessage = "상태변경 불가능";
                        break;
                    }
                }
            }
            collectOrderList.add(collectOrder);
        }catch(Exception e){
            log.error("Reebonz updateOrderToShop Fail => jobId={}\n", jobId, e);
            resultMessage = e.getMessage();
        }

        //최종 response 구성
        OrderJobDto.Request.UpdateCallback updateCallback = new OrderJobDto.Request.UpdateCallback();
        updateCallback.getJobTaskResponseBaseDto().setJobId(jobId);
        updateCallback.getJobTaskResponseBaseDto().setRequestId(updateJobDto.getShopAccount().getRequestId());
        updateCallback.getJobTaskResponseBaseDto().setSuccessFlag(successFlag);
        updateCallback.getJobTaskResponseBaseDto().setMessage(resultMessage);
        updateCallback.setShopAccount(modelMapper.map(updateJobDto.getShopAccount(), ShopAccountDto.Response.class));
        updateCallback.setOrderList(collectOrderList);

        log.info("Reebonz updateOrderToShop JOB END => jobId={}", jobId);

        return new AsyncResult<>(updateCallback);
    }


    @Override
    public ListenableFuture<OrderJobDto.Request.PostConversationCallback> postConversationMessageForOrderToShop(String token, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob) {
        return null;
    }

    @Override
    public ListenableFuture<RegisterDto.Response> updateSaleStatusToShop(String token, long jobId, ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob) {
        return null;
    }

    public String getTokenByApi(GordaSellerTokenDto.Request request){
        ResponseEntity<GordaSellerTokenDto.Response> response = gordaSellerClient.login(request);
        log.info("result code: {}", response.getStatusCode());
        log.info("result body: {}", response.getBody());
        return Objects.requireNonNull(response.getBody()).getAccessToken();
    }

    public GordaSellerTokenDto.Response login(GordaSellerTokenDto.Request request){
        ResponseEntity<GordaSellerTokenDto.Response> response = gordaSellerClient.login(request);
        log.info("result code: {}", response.getStatusCode());
        log.info("result body: {}", response.getBody());
        return response.getBody();
    }

    public SellerInfoResponse getMyInfo(String token){
        ResponseEntity<SellerInfoResponse> response = gordaSellerClient.getMyInfo(token);
        log.info("result code: {}", response.getStatusCode());
        log.info("result body: {}", response.getBody());
        if(response.getStatusCode() != HttpStatus.OK){
            return null;
        }
        return response.getBody();
    }

    /**
     * 배송 관련 변경
     * @param token
     * @param shopOrderId
     * @param courier
     * @param invoiceNumber
     * @return
     */
    public DeliveryDto.Response createDeliveryInfo(String token, String shopOrderId, CourierDto courier, String invoiceNumber){
        try{
            ResponseEntity<DeliveryDto.Response> response = gordaSellerClient.createDelivery(
                    token,
                    new DeliveryDto.Request.Create(Long.parseLong(shopOrderId), courier.getCode(), invoiceNumber)
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 배송 관련 변경
     * @param token
     * @param deliveryId
     * @param courier
     * @param invoiceNumber
     * @return
     */
    public DeliveryDto.Response updateDeliveryInfo(String token, long deliveryId, CourierDto courier, String invoiceNumber){
        try{
            ResponseEntity<DeliveryDto.Response> response = gordaSellerClient.updateDelivery(
                    token,
                    deliveryId,
                    new DeliveryDto.Request.Update(courier.getCode(), invoiceNumber)
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 취소 관련 변경
     * @param token
     * @param shopOrderId
     * @param cancelreturnReasonType
     * @return
     */
    public CancelreturnDto.Response createCancelreturn(String token, String shopOrderId, CancelreturnReasonType cancelreturnReasonType, String detailReason){
        try{
            ResponseEntity<CancelreturnDto.Response> response = gordaSellerClient.createCancelReturns(
                    token,
                    new CancelreturnDto.Request.Create(Long.parseLong(shopOrderId), CancelreturnType.CANCEL, cancelreturnReasonType,detailReason));
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 취소 관련 변경
     * @param token
     * @param cancelreturnId 취소 ID
     * @param cancelreturnReasonType 사유타입
     * @param detailReason 상세사유
     * @return
     */
    public CancelreturnDto.Response updateCancelreturn(String token, long cancelreturnId, CancelreturnReasonType cancelreturnReasonType, String detailReason){
        try {
            ResponseEntity<CancelreturnDto.Response> response = gordaSellerClient.updateCancelReturns(
                    token,
                    cancelreturnId,
                    new CancelreturnDto.Request.Update(cancelreturnReasonType, detailReason));
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                return null;
            }
        }catch(Exception e){
            return null;
        }
    }

    public OrderDto.Request.CollectCallback updateOrderStatusByGordaStatus(String token, String shopOrderId, OrderItemStatus requestGordaOrderItemStatus){
        try {
            ResponseEntity<GordaItemResponse> response = gordaSellerClient.updateOrderStatus(
                    token, shopOrderId, new GordaItemRequest.UpdateStatusRequest(requestGordaOrderItemStatus));
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return convertGordaOrderItemToCollectCallback(response.getBody());
            } else {
                return null;
            }
        }catch(Exception e){
            return null;
        }
    }


    public List<GordaItemResponse> getMyOrders(String token, String collectStartDate) {
        log.info("getMyOrders collectStartDate:{}", collectStartDate);
        String termOption = "PAYMENT_AT";
        String[] sort = {"id","desc"};
        Integer size = Integer.MAX_VALUE-10;

        Long lastDays = null;
        if(!StringUtils.isEmpty(collectStartDate)){ //시작날짜부터 조회

            LocalDate startDate = LocalDate.parse(collectStartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            lastDays = (ChronoUnit.DAYS.between(startDate, LocalDate.now())) +1;
        }
        log.info("lastDays:{}",lastDays);

        ResponseEntity<List<GordaItemResponse>> response = gordaSellerClient.getMyOrders(token,lastDays,termOption, sort,size );
        log.info("result code: {}", response.getStatusCode());
//        log.info("result body: {}", response.getBody());
        return response.getBody();
    }

    public OrderDto.Request.CollectCallback getOrderById(String token, String orderItemId) {
        try {
            ResponseEntity<GordaItemResponse> response = gordaSellerClient.getOrderById(token, orderItemId);
            log.info("result code: {}", response.getStatusCode());
//        log.info("result body: {}", response.getBody());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return convertGordaOrderItemToCollectCallback(response.getBody());
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 쇼핑몰 주문 단일 블럭에 대해서 sellist쪽 주문 블럭으로 매핑
     * @param gordaOrder
     * @return
     */
    public OrderDto.Request.CollectCallback convertGordaOrderItemToCollectCallback(GordaItemResponse gordaOrder) {
        //주문 상태가 취급하지 않는 처리 불필요인 경우에는 스킵한다.
        OrderDto.OrderStatus sellistOrderStatus = convertGordaOrderStatusToOrderStatus(gordaOrder.getStatus(), gordaOrder.getCalculation());
        if(sellistOrderStatus == null)
            return null;

        OrderDto.Request.CollectCallback collectCallback = new OrderDto.Request.CollectCallback();
//        collectCallback.setOrderDate(gordaOrder.getOrder().getPayCompleteAt());
        collectCallback.setOrderDate(gordaOrder.getOrder().getCreatedAt());
        collectCallback.setOrderId(String.valueOf(gordaOrder.getOrder().getId()));
        collectCallback.setOrderUniqueId(String.valueOf(gordaOrder.getId()));
        collectCallback.setStatus(sellistOrderStatus);

        collectCallback.setPostId(String.valueOf(gordaOrder.getShopProduct().getProduct().getId()));
        collectCallback.setBrandName(gordaOrder.getDesignerName());
        collectCallback.setProductName(gordaOrder.getName());
//            collectCallback.setMemo();
//            collectCallback.setClassificationValue();
        collectCallback.setOptionName(gordaOrder.getSizeOptionName());
        collectCallback.setQuantity(String.valueOf(gordaOrder.getQuantity()));
//            collectCallback.setBuyerId();
        collectCallback.setBuyerName(gordaOrder.getOrder().getInfo().getName());
//            collectCallback.setBuyerPhoneNumber();
        collectCallback.setBuyerMobilePhoneNumber(gordaOrder.getOrder().getInfo().getCellphone());
        collectCallback.setBuyerEmail(gordaOrder.getOrder().getInfo().getEmail());

        collectCallback.setRecipientName(gordaOrder.getOrder().getInfo().getReceiverName());
        collectCallback.setRecipientPhoneNumber(gordaOrder.getOrder().getInfo().getReceiverTelephone());
        collectCallback.setRecipientMobilePhoneNumber(gordaOrder.getOrder().getInfo().getReceiverCellphone());
//            collectCallback.setRecipientEmail();
        collectCallback.setRecipientZipCode(gordaOrder.getOrder().getInfo().getReceiverRoadCode());
        collectCallback.setRecipientAddress(gordaOrder.getOrder().getInfo().getReceiverRoadAddress() + " " + gordaOrder.getOrder().getInfo().getReceiverExtraRoadAddress());
//            collectCallback.setDepositor();
//            collectCallback.setPersonalClearanceCode();

        collectCallback.setPrice(gordaOrder.getPrice().longValue()); // 상품가격
        collectCallback.setPaymentPrice(gordaOrder.getSettlePriceAmount().longValue());//결제금액(공급가)
        collectCallback.setCouponDiscountPrice(gordaOrder.getSettlePriceAmount().longValue());//쿠폰가(쿠폰 적용 할인된 금액)
        collectCallback.setCouponPrice((gordaOrder.getCouponDiscountAmount().add(gordaOrder.getPromotionDiscountAmount())).longValue());

        if(gordaOrder.getInvoice() != null && gordaOrder.getInvoice().getCompany() != null){
            collectCallback.setCourierCode(String.valueOf(gordaOrder.getInvoice().getCompany().getId()));//고르다 택배사 코드
            collectCallback.setCourierName(gordaOrder.getInvoice().getCompany().getName());
//                collectCallback.setCourierCustomName(); //직접입력한 택배사 이름
            collectCallback.setTrackingNumber(gordaOrder.getInvoice().getInvoiceNumber());
            collectCallback.setShopDeliveryId(gordaOrder.getInvoice().getId());
        }
        collectCallback.setDeliveryMessage(gordaOrder.getOrder().getInfo().getMemo());// 배송 메세지

//            collectCallback.setGift();
//            collectCallback.setRequirements();

        //고르다쪽은 기본 무료배송정책
        collectCallback.setDeliveryFeeType("무료배송");
        collectCallback.setDeliveryType("국내");
        collectCallback.setDeliveryFee(String.valueOf(gordaOrder.getOrder().getDeliveryFeeAmount()));

        //API쪽에서 상태에 따른 처리를 하기 때문에 reason값을 동일하게 넣어줌
        if(gordaOrder.getCancelreturn() != null){
            String reasonDescription = gordaOrder.getCancelreturn().getReasonType().getDescription();
            collectCallback.setReturnReason(reasonDescription);// 반품 사유
            collectCallback.setExchangeReason(reasonDescription);// 교환 사유
            collectCallback.setPurchaseCancellationReason(reasonDescription);// 구매취소 사유
            collectCallback.setSaleCancellationReason(reasonDescription);// 판매취소 사유
            collectCallback.setShopCancelreturnId(gordaOrder.getCancelreturn().getId());
        }

//            collectCallback.setOfficialSku(); //공식 SKU(리본즈SKU)
//            collectCallback.setCustomizedSku(); //판매자 SKU

//            collectCallback.setExchangeCourierCode();
//            collectCallback.setExchangeCourierName();
//            collectCallback.setExchangeCourierCustomName();
//            collectCallback.setExchangeTrackingNumber();

//            collectCallback.setReturnCourierCode();
//            collectCallback.setReturnCourierName();
//            collectCallback.setReturnCourierCustomName();
//            collectCallback.setReturnTrackingNumber();

        if(!ObjectUtils.isEmpty(gordaOrder.getCalculation())){
            collectCallback.setCalculateDate(gordaOrder.getCalculation().getExpectedPaymentAt().toString());  //정산지급 예정일
            collectCallback.setCalculateAmount(String.valueOf(gordaOrder.getCalculation().getPaymentAmount())); //정산(예정)금액
        }

        return collectCallback;
    }

    /**
     * 쇼핑몰 주문 목록을 가져와서 sellist 콜백 목록으로 매핑한다.
     * @param gordaOrderList
     * @return
     */
    public List<OrderDto.Request.CollectCallback> convertGordaOrderItemsToCollectCallback(List<GordaItemResponse> gordaOrderList) {
        List<OrderDto.Request.CollectCallback> collectCallbackList = new ArrayList<>();
        for(GordaItemResponse gordaOrder : gordaOrderList){
            OrderDto.Request.CollectCallback collectCallback = convertGordaOrderItemToCollectCallback(gordaOrder);
            if(collectCallback == null)
                continue;
            collectCallbackList.add(collectCallback);
        }

        return collectCallbackList;
    }

    /**
     * 고르다의 주문상태로 부터 sellist 입력 주문상태로 변환한다.
     * @param gordaOrderItemStatus
     * @param calculation
     * @return
     */
    public OrderDto.OrderStatus convertGordaOrderStatusToOrderStatus(OrderItemStatus gordaOrderItemStatus, GordaItemResponse.Calculation calculation){
        switch (gordaOrderItemStatus){
            case BEFORE_PAYMENT: //결제 진행전 - 취급안함
                return null;

            case DEPOSIT_WAITING: //입금대기 - 취급안함
                return null;

            case DEPOSIT_AUTO_CANCELLATION: //입금대기 자동취소 - 취급안함
                return null;

            case PAYMENT_FINISHED: //결제완료
                return OrderDto.OrderStatus.PAYMENT_COMPLETE;

            case PAYMENT_CANCELING: //결제취소요청
                return OrderDto.OrderStatus.BUY_CANCEL_REQUEST;

            case PAYMENT_CANCELLATION: //결제취소
                return OrderDto.OrderStatus.BUY_CANCEL_COMPLETE;

            case PRODUCT_PREPARATION: //상품준비
                return OrderDto.OrderStatus.DELIVERY_READY;

            case SHIPPING: //배송
                return OrderDto.OrderStatus.DELIVERY;

            case DELIVERY_COMPLETED: //배송완료
                return OrderDto.OrderStatus.DELIVERY_COMPLETE;

            case PURCHASE_COMPLETED: //구매완료
            {
                //현재 구매확정 프로세스와 정산관리쪽 프로세스가 없는 이유로 정산쪽 status를 확인해서 처리함
                if(!ObjectUtils.isEmpty(calculation) && !ObjectUtils.isEmpty(calculation.getStatus())){
                    switch(calculation.getStatus()){
                        case READY:
                            return OrderDto.OrderStatus.CALCULATION_SCHEDULE;
                        case HOLD:
                            return OrderDto.OrderStatus.CALCULATION_DELAY;
                        case COMPLETED:
                            return OrderDto.OrderStatus.CALCULATION_COMPLETE;
                    }
                }
                return OrderDto.OrderStatus.CALCULATION_SCHEDULE;
            }

            case RETURN_RECEIVED: //반품접수
                return OrderDto.OrderStatus.RETURN_REQUEST;

            case RETURN_RECEIVED_COMPLETED: //반품접수완료
                return OrderDto.OrderStatus.RETURN_CONFIRM;

            case RETURN_PRODUCT_PICK_UP: //반품상품수거
                return OrderDto.OrderStatus.RETURN_CONFIRM;

            case RETURN_PRODUCT_PICK_UP_COMPLETED: //반품상품수거완료
                return OrderDto.OrderStatus.RETURN_CONFIRM;

            case RETURN_PRODUCT_CHECK: //반품상품검수
                return OrderDto.OrderStatus.RETURN_CONFIRM;

            case RETURN_COMPLETED: //반품완료
                return OrderDto.OrderStatus.RETURN_COMPLETE;


            case EXCHANGING: //교환
                return OrderDto.OrderStatus.EXCHANGE_REQUEST;

            case EXCHANGE_PICK_COMPLETE: //교환수거완료
                return OrderDto.OrderStatus.DELIVERY;

            case EXCHANGE_RESEND: //교환재발송
                return OrderDto.OrderStatus.DELIVERY;

            case EXCHANGE_WITHDRAW: //교환거절
                return OrderDto.OrderStatus.DELIVERY;

            case EXCHANGE_COMPLETE: //교환완료
                return OrderDto.OrderStatus.DELIVERY_COMPLETE;

            default:
                return null;
        }
    }


}
