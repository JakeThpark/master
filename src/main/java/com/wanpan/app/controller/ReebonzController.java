package com.wanpan.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.dto.reebonz.*;
import com.wanpan.app.repository.JobRepository;
import com.wanpan.app.service.BrandService;
import com.wanpan.app.service.ShopAccountService;
import com.wanpan.app.service.job.CollectOrderBaseConversationFromShopTaskService;
import com.wanpan.app.service.job.JobTaskService;
import com.wanpan.app.service.job.PostOrderConversationToShopTaskService;
import com.wanpan.app.service.reebonz.ReebonzBrandService;
import com.wanpan.app.service.reebonz.ReebonzCategoryService;
import com.wanpan.app.service.reebonz.ReebonzService;
import com.wanpan.app.service.reebonz.ReebonzWebPageService;
import com.wanpan.app.service.reebonz.constant.ReebonzOrderListType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping({"/reebonz"})
@AllArgsConstructor
public class ReebonzController {
    private final ShopAccountService shopAccountService;
    private final BrandService brandService;
    private final ReebonzService reebonzService;
    private final ReebonzBrandService reebonzBrandService;
    private final ReebonzCategoryService reebonzCategoryService;
    private final ReebonzWebPageService reebonzWebPageService;
    private final JobRepository jobRepository;
    private final JobTaskService jobTaskService;
    private final CollectOrderBaseConversationFromShopTaskService collectOrderBaseConversationFromShopTaskService;
    private final PostOrderConversationToShopTaskService postOrderConversationToShopTaskService;

    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;

    /*
     * 리본즈 브랜드 초기화 작업에 사용된다.
     * 리본즈의 브랜드 리스트를 끌어와서
     * 기존에 등록된 브랜드와 동일 이름일 경우에 매핑을 한다.
     * 동일 이름이 없을 경우는 수동처리를 위한 테이블에 저장한다.
     */
    @PostMapping(value = "/brand-mapping")
    @ApiOperation(value="리본즈 브랜드 초기 목록 매핑", notes = "리본즈 브랜드를 기존의 브랜드들에 기준 데이타 매핑한다")
    public ResponseEntity<List<BrandDto>> mappingBrand(
            @RequestBody ReebonzJobBaseRequest reebonzJobBaseRequest) {
        return ResponseEntity.ok(brandService
                .mappingEachShopBrandByShopType("REEBONZ", reebonzJobBaseRequest.getShopAccountId()));
    }

    /*
     * 리본즈 브랜드 목록
     */
    @GetMapping(value = "/brands")
    @ApiOperation(value="리본즈 브랜드 목록", notes = "리본즈 브랜드 목록을 보여준다")
    public ResponseEntity<List<BrandDto>> getBrandList(
            @ApiParam(value = "쇼핑몰 계정 저장 ID", required = true) @RequestParam(value = "shop-account-id") long shopAccountId,
            @ApiParam(value = "쇼핑몰 브랜드 검색어", required = true) @RequestParam(value = "search-name") String searchName)
            throws GeneralSecurityException, IOException {
        String token = shopAccountService.getTokenByShopAccountId(shopAccountId);
        return ResponseEntity
                .ok(reebonzBrandService.getBrandListBySearchName(token, searchName));
    }


    /*
     * 리본즈 category parameter newCategory: value = true, new version(3depth)
     * 리본즈 category parameter parent_id: Gender: 1, Category: 4
     */
    @GetMapping(value = "/categories")
    @ApiOperation(value="리본즈 카테고리 목록", notes = "리본즈 카테고리 목록")
    public ResponseEntity<List<ReebonzCategory>> getCategoryList(
            @ApiParam(value = "쇼핑몰 계정 저장 ID", required = true) @RequestParam(value="shop-account-id", required = true) long shopAccountId,
            @ApiParam(value = "리본즈 새 카테고리 여부: new version(3depth)", required = true) @RequestParam(value="new_category", defaultValue = "true") boolean newCategory,
            @ApiParam(value = "Gender: 1, Category: 4", required = true) @RequestParam(value="parent_id") int parentId) throws IOException {

        return ResponseEntity.ok(reebonzCategoryService.getCategoryListByShopAccountId(shopAccountId, newCategory, parentId));
    }

    /*
     * 리본즈에서 성별, 제품 카테고리 목록을 읽어와서 shop_category에 저장한다.
     */
    @PostMapping(value = "/categories")
    @ApiOperation(value="리본즈 카테고리 저장", notes = "리본즈 카테고리를 저장한다")
    public ResponseEntity<List<ReebonzCategory>> createCategory(
            @RequestBody ReebonzJobBaseRequest reebonzJobBaseRequest) throws IOException {

        return ResponseEntity.ok(
                reebonzCategoryService.createCategory(
                        reebonzJobBaseRequest.getShopAccountId()
                )
        );
    }

    /*
     * TODO:ORDER ITEM 관련 API들 필요
     */

    @GetMapping(value = "/product-meta-info")
    public ResponseEntity<List<ProductMetaInfo>> getProductMetaInfoList(
            @RequestParam(value = "sku-code", required = true) String skuCode) throws IOException {

        return ResponseEntity.ok(reebonzService.getProductMetaInfoByCode(skuCode));
    }

    @PostMapping(value = "/delivery")
    public ResponseEntity createOrUpdateDelivery(
            @RequestBody(required = true) DeliveryRequest deliveryRequest) throws IOException {

        return ResponseEntity.ok(reebonzService.createOrUpdateDelivery(deliveryRequest));
    }

    @GetMapping(value = "/products")
    public ResponseEntity<List<Product>> getProductList(
            @ApiParam("true/false - On sale, Not on sal") @RequestParam(value="available", required = false) boolean available,
            @ApiParam("true/false - soldout ") @RequestParam(value="soldout", required = false) boolean soldout) throws IOException {

        return ResponseEntity.ok(reebonzService.getProductList(available, soldout));
    }

    @GetMapping(value = "/products/{product-id}")
    public ResponseEntity<Product> getProductById(
            @ApiParam(value = "리본즈 상품 등록 아이디", required = true) @PathVariable("product-id") int reebonzProductId
    ) throws IOException {

        return ResponseEntity.ok(reebonzService.getProductById(reebonzProductId));
    }

    @DeleteMapping(value = "/products/{product-id}/stocks/{stock-id}")
    public ResponseEntity<ReebonzBaseResponse<StockDeleteRequest>> deleteStockById(
            @ApiParam(value = "리본즈 상품 등록 아이디", required = true) @PathVariable("product-id") int reebonzProductId,
            @ApiParam(value = "리본즈 상품의 옵션별 재고정보 아이디", required = true) @PathVariable("stock-id") int reebonStockId) throws IOException {

        return ResponseEntity.ok(reebonzService.deleteProductStock(reebonzProductId, reebonStockId));
    }

    /*
     * 리본즈 api페이지 주문수집(test를 위해서 put로 구현)
     */
    @PutMapping(value = "/collect-order")
    @ApiOperation(value="리본즈 API 주문수집", notes = "주문수집")
    public ResponseEntity<OrderJobDto.Request.CollectCallback> collectOrder(
            @ApiParam(value = "뤱,API,로그인 토큰", required = true) @RequestParam(value = "token") String token,
            @ApiParam(value = "리본즈 API 사용 여부", required = true) @RequestParam (value = "use-api") boolean useApi,
            @ApiParam(value = "조회 계정 정보", required = true) @RequestBody ShopAccountDto.Request shopAccount)
            throws IOException {
        if(useApi){
            return ResponseEntity.ok(reebonzService.collectOrderFromShopByApi(token, 0, OrderJobDto.OrderProcessStatus.COMPLETE, shopAccount));
        }else{
            return ResponseEntity.ok(reebonzService.collectOrderFromShopByWebPage(token, 0, OrderJobDto.OrderProcessStatus.COMPLETE, shopAccount));
        }
    }


    /*
     * 리본즈 브랜드 목록
     */
    @GetMapping(value = "/web-login")
    @ApiOperation(value="리본즈 web login session", notes = "리본즈 웹 로그인 세션을 얻어온다")
    public ResponseEntity<String> getWebLoginSession(
            @ApiParam(value = "쇼핑몰 계정", required = true) @RequestParam(value = "shop-login-id") String shopLoginId,
            @ApiParam(value = "쇼핑몰 패스워드", required = true) @RequestParam(value = "shop-login-password") String password)
            throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.getToken(shopLoginId, password));
    }

    /*
     * 리본즈 상품문의 작성 테스트
     */
    @GetMapping(value = "/web-qna-reply")
    @ApiOperation(value="리본즈 상품문의 답글달기", notes = "리본즈 상품문의에 답변을 작성한다.")
    public ResponseEntity<String> qnaReply(
            @ApiParam(value = "웹세션토큰", required = true) @RequestParam(value = "web-token") String webToken,
            @ApiParam(value = "요청Job Data", required = true) @RequestParam(value = "qna-post-job") ShopQnaJobDto.Request.PostJob qnaPostJobDto
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.postAnswerForQna(webToken, qnaPostJobDto));
    }

    /*
     * 리본즈 상품문의 목록
     */
    @GetMapping(value = "/web-collect-qna")
    @ApiOperation(value="리본즈 상품 문의수집", notes = "상품 문의수집")
    public ResponseEntity<String> getQuestion(
            @ApiParam(value = "로그인 세션", required = true) @RequestParam(value = "web-token") String webToken,
            @ApiParam(value = "상품문의 검색범위", required = true) @RequestParam(value = "search-is-reply") String searchIsReply)
            throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.collectQna(webToken, searchIsReply));
    }

    /*
     * 리본즈 웹페이지 주문수집
     */
    @GetMapping(value = "/web-collect-order/{order-id}")
    @ApiOperation(value="리본즈 주문아이디 웹,엑셀 주문수집", notes = "아이디 주문수집")
    public ResponseEntity<List<OrderDto.Request.CollectCallback>> collectOrderFromWebByOrderId(
            @ApiParam(value = "로그인 세션", required = true) @RequestParam(value = "web-token") String webToken,
            @ApiParam(value = "주문 아이디", required = true) @PathVariable(value = "order-id") String orderId)
            throws IOException {
        return ResponseEntity.ok(reebonzService.getOrderFromWebAndExcelByOrderId(webToken, orderId));
    }

    /*
     * 리본즈 웹페이지 주문수집
     */
    @GetMapping(value = "/web-collect-order")
    @ApiOperation(value="리본즈 웹페이지 주문수집", notes = "상품 주문수집")
    public ResponseEntity<List<OrderDto.Request.CollectCallback>> collectOrderByWeb(
            @ApiParam(value = "로그인 세션", required = true) @RequestParam(value = "web-token") String webToken,
            @ApiParam(value = "조회 리스트 타입", required = true) @RequestParam(value = "order-list-type") ReebonzOrderListType reebonzOrderListType,
            @ApiParam(value = "주문 아이디") @RequestParam(value = "order-id", required = false) String orderId)
            throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.collectOrderProcessingFromWeb(webToken, reebonzOrderListType, orderId));
    }

    /*
     * 리본즈 웹페이지 주문 클레임수집
     */
    @GetMapping(value = "/web-collect-order-claim")
    @ApiOperation(value="리본즈 웹페이지 주문수집", notes = "상품 주문수집")
    public ResponseEntity<List<OrderDto.Request.CollectCallback>> collectOrderClaimByWeb(
            @ApiParam(value = "로그인 세션", required = true) @RequestParam(value = "web-token") String webToken,
            @ApiParam(value = "조회 리스트 타입", required = true) @RequestParam(value = "order-list-type") ReebonzOrderListType reebonzOrderListType,
            @ApiParam(value = "주문 아이디", required = true) @RequestParam(value = "order-id") String orderId)
            throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.collectOrderClaimFromWeb(webToken, reebonzOrderListType, orderId));
    }



    /**
     * 리본즈 웹페이지 주문상태 변경(주문확인 - 배송준비중)
     */
    @PutMapping(value = "/web-order-confirm")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(주문확인,배송준비중)", notes = "리본즈 웹페이지 주문상태변경(주문확인,배송준비중)")
    public ResponseEntity<String> updateOrderConfirm(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderConfirm(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 배송중(송장번호 입력)
     */
    @PutMapping(value = "/web-order-delivery")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(배송중,송장저장)", notes = "리본즈 웹페이지 주문상태변경(배송중,송장저장)")
    public ResponseEntity<String> updateOrderDelivery(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderDelivery(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 반품확인
     */
    @PutMapping(value = "/web-order-return-confirm")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(반품확인)", notes = "리본즈 웹페이지 주문상태변경(반품확인)")
    public ResponseEntity<String> updateOrderReturnConfirm(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderReturnConfirm(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 반품완료
     */
    @PutMapping(value = "/web-order-return-complete")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(반품완료)", notes = "리본즈 웹페이지 주문상태변경(반품완료)")
    public ResponseEntity<String> updateOrderReturnComplete(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderReturnComplete(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 반품거절
     */
    @PutMapping(value = "/web-order-return-reject")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(반품거절)", notes = "리본즈 웹페이지 주문상태변경(반품거절)")
    public ResponseEntity<String> updateOrderReturnReject(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderReturnReject(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 판매취소,품절
     */
    @PutMapping(value = "/web-order-sell-cancel")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(판매취소)", notes = "리본즈 웹페이지 주문상태변경(판매취소)")
    public ResponseEntity<String> updateOrderSellCancel(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderSellCancel(webToken, updateJob));
    }

    /*
     * 리본즈 웹페이지 주문상태 변경 - 판매취소,품절
     */
    @PutMapping(value = "/web-order-buy-cancel-confirm")
    @ApiOperation(value="리본즈 웹페이지 주문상태변경(구매취소승인)", notes = "리본즈 웹페이지 주문상태변경(구매취소승인)")
    public ResponseEntity<String> updateOrderBuyCancelConfirm(
            @RequestParam String webToken,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(reebonzWebPageService.updateOrderBuyCancelConfirm(webToken, updateJob));
    }

    /*
     * 리본즈 브랜드 목록
     */
    @GetMapping(value = "/sale-updatable")
    @ApiOperation(value="리본즈 판매수정 가능 여부", notes = "리본즈 판매수정 가능 여부")
    public ResponseEntity<Boolean> getSaleUpdatableFlag(
            @ApiParam(value = "세션", required = true) @RequestParam(value = "webToken") String webToken,
            @ApiParam(value = "상품번호", required = true) @RequestParam(value = "productNumber") String productNumber)
            throws IOException {
        return ResponseEntity.ok(reebonzService.isSaleUpdatable(webToken, productNumber));
    }

    @Data
    @NoArgsConstructor
    public static class ReebonzJobBaseRequest {
        private long shopAccountId;
        private boolean newCategory;
        private int parentId;
    }


}
