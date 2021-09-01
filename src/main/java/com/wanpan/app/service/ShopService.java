package com.wanpan.app.service;

import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.ShopAccountToken;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.List;

/*
 * 모든 쇼핑몰 기능을 선언해 놓은 인터페이스
 */

public interface ShopService {
    ShopAccountDto.Response checkSignIn(
            String loginId,
            String password,
            ShopAccountDto.Response shopAccountResponseDto
    ) throws IOException;

    boolean isKeepSignIn(String token, String accountId, ShopAccountToken.Type tokenType);

    String getToken(String accountId, String password, ShopAccountToken.Type tokenType);

    List<BrandDto> getBrandList(String token);

    List<CategoryDto> getCategoryList(String token);

    @Async
    @Transactional
    ListenableFuture<RegisterDto.Response> postSaleToShop(String token, Job job, OnlineSaleDto onlinesaleDto);

    @Async
    @Transactional
    ListenableFuture<RegisterDto.Response> updateSaleToShop(String token, long jobId, OnlineSaleDto onlineSaleDto) throws IOException;

    /**
     * 상품 문의에 대해서 각 서비스별로 수집
     */
    @Async
    @Transactional
    ListenableFuture<ShopQnaJobDto.Request.CollectCallback> collectQnAFromShop(String token, long jobId, ShopQnaJobDto.QuestionStatus questionStatus, ShopAccountDto.Request request);

    /**
     * 주문 대화에 대해서 각 서비스별로 수집
     */
    @Async
    @Transactional
    ListenableFuture<OrderBaseConversationJobDto.Request.CollectCallback> collectOrderConversationFromShop(String token, long jobId, OrderBaseConversationJobDto.OrderConversationStatus orderConversationStatus, ShopAccountDto.Request request);

    /**
     * 상품 문의에 대한 답변을 하고 해당 문의번호에 해당하는 질문 답변에 대해서 결과를 다시 수집한다.
     */
    @Async
    @Transactional
    ListenableFuture<ShopQnaJobDto.Request.PostCallback> postAnswerForQnaToShop(String token, long jobId, ShopQnaJobDto.Request.PostJob postJobDto);

    /**
     * 상품 삭제
     * @param token
     * @param jobId
     * @param postJobDto
     * @return
     */
    @Async
    @Transactional
    ListenableFuture<RegisterDto.Response> deleteShopSale(String token, long jobId, ShopSaleJobDto.Request.DeleteSaleJob postJobDto);

    /**
     * 상품 주문에 대해서 각 서비스별로 수집
     */
    @Async
    @Transactional
    ListenableFuture<OrderJobDto.Request.CollectCallback> collectOrderFromShop(String token, long jobId, OrderJobDto.OrderProcessStatus orderProcessStatus, ShopAccountDto.Request request);

    /**
     * 상품 주문 상태변경
     */
    @Async
    @Transactional
    ListenableFuture<OrderJobDto.Request.UpdateCallback> updateOrderToShop(String token, long jobId, OrderJobDto.Request.UpdateJob updateJobDto);

    /**
     * 주문 대화 입력
     * @param token
     * @param jobId
     * @param postConversationJob
     * @return
     */
    @Async
    @Transactional
    ListenableFuture<OrderJobDto.Request.PostConversationCallback> postConversationMessageForOrderToShop(String token, long jobId, OrderJobDto.Request.PostConversationJob postConversationJob);

    /**
     * 쇼핑몰 판매 상태 변경
     * @param token
     * @param jobId
     * @param updateSaleStatusJob
     * @return
     */
    @Async
    @Transactional
    ListenableFuture<RegisterDto.Response> updateSaleStatusToShop(String token, long jobId, ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob);

}
