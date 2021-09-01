package com.wanpan.app.controller;

import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.CategoryDto;
import com.wanpan.app.dto.feelway.FeelwaySellingProduct;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.service.BrandService;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.ShopServiceFactory;
import com.wanpan.app.service.feelway.FeelwayRequestPageService;
import com.wanpan.app.service.feelway.FeelwayService;
import com.wanpan.app.service.feelway.FeelwayTestEngineService;
import com.wanpan.app.service.feelway.parser.FeelwayProductParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping({"/feelway"})
@AllArgsConstructor
public class FeelwayController {
    private final ShopServiceFactory shopServiceFactory;
    private final BrandService brandService;
    private final FeelwayService feelwayService;

    @GetMapping(value = "/brands")
    public ResponseEntity<List<BrandDto>> getBrandList(
            @RequestParam String session
    ) {
        ShopService shopService = shopServiceFactory.getShopService("FEELWAY");
        return ResponseEntity.ok(shopService.getBrandList(session));
    }

    /*
     * 필웨이 브랜드 리스트를 끌어와서 기준 테이블과 비교 후 매핑한다.
     */
    @PostMapping(value = "/brand-maps")
    public ResponseEntity<List<BrandDto>> mappingBrand(
            @RequestBody FeelwayJobBaseRequest feelwayJobBaseRequest) {
        return ResponseEntity.ok(brandService
                .mappingEachShopBrandByShopType("FEELWAY", feelwayJobBaseRequest.getShopAccountId()));
    }

//    @PostMapping(value = "/sale")
//    public ResponseEntity<ListenableFuture<RegisterDto.Response>> registerSale(
//            @RequestParam String session
//    ) {
//        ShopService shopService = shopServiceFactory.getShopService(Shop.Type.FEELWAY);
//        return ResponseEntity.ok(shopService.postSaleToShop(session, 0, null));
//    }

//    @DeleteMapping(value = "/sale")
//    public ResponseEntity<Boolean> deleteSale(
//            @RequestParam String session,
//            @RequestParam String productId
//    ) {
//        ShopService shopService = shopServiceFactory.getShopService(Shop.Type.FEELWAY);
//        return ResponseEntity.ok(shopService.deleteProductFromShop(session, productId));
//    }

    @GetMapping(value = "/category")
    public ResponseEntity<List<CategoryDto>> registerCategory(
            @RequestParam String session
    ) {
        ShopService shopService = shopServiceFactory.getShopService("FEELWAY");
        return ResponseEntity.ok(shopService.getCategoryList(session));
    }

    /*
     * 필웨이 상품문의 목록
     */
    private final FeelwayTestEngineService feelwayTestEngineService;

    @GetMapping(value = "/collect-question")
    @ApiOperation(value = "필웨이 상품 문의수집", notes = "상품 문의수집")
    public ResponseEntity<String> getQuestion(
            @ApiParam(value = "삽의 accountId", required = true) @RequestParam(value = "shop-account-id") long shopAccountId,
            @ApiParam(value = "질문 상태") @RequestParam( value = "question-status", required = false) ShopQnaJobDto.QuestionStatus questionStatus,
            @ApiParam(value = "질문자ID") @RequestParam(value = "ask-id", required = false) String askId)
            throws IOException, GeneralSecurityException {
        return ResponseEntity.ok(feelwayTestEngineService.collectQna(shopAccountId, questionStatus, askId));
    }

    @GetMapping(value = "/order-excel")
    public ResponseEntity<List<OrderDto.Request.CollectCallback>> getOrderFromExcel(
            @RequestParam String session
    ) throws IOException {
        return ResponseEntity.ok().body(feelwayService.getOrderFromExcel(session));
    }

    @GetMapping(value = "/order-page")
    public ResponseEntity<List<OrderDto.Request.CollectCallback>> getOrderFromPage(
            @RequestParam String session
    ) throws IOException {
        return ResponseEntity.ok().body(feelwayService.getOrderFromPage(session));
    }

    @PostMapping(value = "/order-conversation")
    public ResponseEntity<OrderJobDto.Request.PostConversationCallback> postOrderConversationMessage(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.PostConversationJob postConversationJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.postOrderBaseConversationMessage(session, 0, postConversationJob));
    }

    //주문 취소 테스트 API
    @PutMapping(value = "/order-cancel")
    public ResponseEntity<String> updateCancelOrder(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateOrderToSellCancel(session, 0, updateJob));
    }

    //주문 배송 테스트 API(주문배송)
    @PutMapping(value = "/order-delivery")
    public ResponseEntity<String> updateOrderDelivery(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateOrderToDelivery(session, 0, updateJob,""));
    }

    //주문 배송 테스트 API(배송 송장 업데이트)
    @PutMapping(value = "/order-delivery-change")
    public ResponseEntity<String> updateDeliveryInfo(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateDeliveryInfo(session, 0, updateJob));
    }

    //반품 동의 테스트 API
    @PutMapping(value = "/return-confirm")
    public ResponseEntity<String> updateReturnConfirm(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateReturnConfirm(session, 0, updateJob,""));
    }

    //반품 거절 테스트 API
    @PutMapping(value = "/return-reject")
    public ResponseEntity<String> updateReturnReject(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateReturnReject(session, 0, updateJob,""));
    }

    //반품 거절 테스트 API
    @PutMapping(value = "/return-complete")
    public ResponseEntity<String> updateReturnComplete(
            @RequestParam String session,
            @RequestBody OrderJobDto.Request.UpdateJob updateJob
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.updateReturnComplete(session, 0, updateJob,""));
    }


    private final FeelwayRequestPageService feelwayRequestPageService;
    //마지막 상품 가져오기 test
    @GetMapping(value = "/latest-selling-product")
    public ResponseEntity<FeelwaySellingProduct> getlatestSellingProduct(
            @RequestParam String token
    ) throws IOException {
        FeelwaySellingProduct feelwaySellingProduct = FeelwayProductParser.getlatestSellingProduct(feelwayRequestPageService.getSellingProduct(token));
        if(ObjectUtils.isEmpty(feelwaySellingProduct)){
            return ResponseEntity.ok(new FeelwaySellingProduct());
        }else{
            return ResponseEntity.ok(feelwaySellingProduct);
        }
    }

    //마지막 상품 가져오기 test
    @GetMapping(value = "/selling-product-count")
    public ResponseEntity<Integer> getSellingProductCount(
            @RequestParam String token
    ) throws IOException {
        int feelwaySellingProductCount = FeelwayProductParser.getSellingProductCount(feelwayRequestPageService.getSellingProduct(token));
        return ResponseEntity.ok(feelwaySellingProductCount);
    }

    //상품 정보 읽어 오기
    @GetMapping(value = "/product/{productNumber}")
    public ResponseEntity<FeelwaySellingProduct> getlatestSellingProduct(
            @PathVariable String productNumber,
            @RequestParam String token
    ) throws IOException {
        return ResponseEntity.ok(feelwayService.getShopSaleStatudByProductNumber(token, productNumber));
    }

    @Data
    public static class FeelwayJobBaseRequest {
        private long shopAccountId;
    }
}
