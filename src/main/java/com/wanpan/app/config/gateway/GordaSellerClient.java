package com.wanpan.app.config.gateway;

import com.wanpan.app.config.FeignCamelConfiguration;
import com.wanpan.app.dto.godra.GordaSellerTokenDto;
import com.wanpan.app.dto.godra.seller.delivery.DeliveryDto;
import com.wanpan.app.dto.godra.seller.inquiry.InquiryDto;
import com.wanpan.app.dto.godra.seller.my.SellerInfoResponse;
import com.wanpan.app.dto.godra.seller.order.CancelreturnDto;
import com.wanpan.app.dto.godra.seller.order.GordaItemRequest;
import com.wanpan.app.dto.godra.seller.order.GordaItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "gorda-seller", url = "${gorda.client.seller-url}", configuration = FeignCamelConfiguration.class, decode404 = true) //https://api.gordastyle.com"
public interface GordaSellerClient {
    //Login token(인증)=================================================
    @PostMapping("/oauth/tokens")
    ResponseEntity<GordaSellerTokenDto.Response> login(
            @RequestBody GordaSellerTokenDto.Request loginRequest
    );

    @GetMapping("/me/info")
    ResponseEntity<SellerInfoResponse> getMyInfo(
            @RequestHeader("Authorization") String token
    );

    //orderItems(주문상품)============================================
    @GetMapping("/me/orderItems")
    ResponseEntity<List<GordaItemResponse>> getMyOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam("lastDays") Long lastDays,
            @RequestParam("termOption") String termOption,
            @RequestParam("sort") String[] sort,
            @RequestParam("size") Integer size
    );

    @GetMapping("/me/orderItems/{id}")
    ResponseEntity<GordaItemResponse> getOrderById(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String id
    );

    @PutMapping("/me/orderItems/{id}")
    ResponseEntity<GordaItemResponse> updateOrderStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String orderItemId,
            @RequestBody GordaItemRequest.UpdateStatusRequest updateStatusRequest
    );

    //deliveries(배송)===============================================
    @PostMapping("/me/deliveries")
    ResponseEntity<DeliveryDto.Response> createDelivery(
            @RequestHeader("Authorization") String token,
            @RequestBody DeliveryDto.Request.Create deliveryRequest
    );

    @PutMapping("/me/deliveries/{id}")
    ResponseEntity<DeliveryDto.Response> updateDelivery(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") long deliveryId,
            @RequestBody DeliveryDto.Request.Update deliveryRequest
    );

    //CancelReturns(취소/반품/교환)========================================
    @PostMapping("/me/cancelreturns")
    ResponseEntity<CancelreturnDto.Response> createCancelReturns(
            @RequestHeader("Authorization") String token,
            @RequestBody CancelreturnDto.Request.Create cancelReturnRequest
    );

    @PutMapping("/me/cancelreturns/{id}")
    ResponseEntity<CancelreturnDto.Response> updateCancelReturns(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") long cancelReturnId,
            @RequestBody CancelreturnDto.Request.Update cancelReturnRequest
    );

    //inquiries(상품문의)=================================================
    @GetMapping("/me/inquiries")
    ResponseEntity<List<InquiryDto.Response>> getInquiries(
            @RequestHeader("Authorization") String token,
            @RequestParam("isAnswered") boolean isAnswered,
            @RequestParam("lastDays") Long lastDays,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam("sort") String[] sort
    );

    @GetMapping("/me/inquiries/{id}")
    ResponseEntity<InquiryDto.Response> getInquiryById(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") long cancelReturnId
    );

    @PostMapping("/me/inquiries/{id}/answer")
    ResponseEntity<InquiryDto.Response> createAnswer(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") long inquiryId,
            @RequestBody InquiryDto.Request.CreateAnswer createAnswerRequest
    );

    @PutMapping("/me/inquiries/{id}/answer/{answerId}")
    ResponseEntity<InquiryDto.Response> updateAnswer(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") long inquiryId,
            @PathVariable("answerId") long answerId,
            @RequestBody InquiryDto.Request.UpdateAnswer updateAnswerRequest
    );


    //Product==============================================
    @PostMapping("/me/products/")
    ResponseEntity<InquiryDto.Response> mappingRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody InquiryDto.Request.CreateAnswer createAnswerRequest
    );


}
