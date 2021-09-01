package com.wanpan.app.controller;

import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.godra.GordaSellerTokenDto;
import com.wanpan.app.dto.godra.seller.my.SellerInfoResponse;
import com.wanpan.app.dto.godra.seller.order.GordaItemResponse;
import com.wanpan.app.dto.job.order.OrderDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.service.gorda.GordaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping({"/gorda"})
@AllArgsConstructor
public class GordaController {
    private final GordaService gordaService;

    @PostMapping(value = "/oauth/tokens")
    public ResponseEntity<GordaSellerTokenDto.Response> login(
            @RequestBody GordaSellerTokenDto.Request loginRequest
    ){
        return ResponseEntity.ok(gordaService.login(loginRequest));
    }

    @GetMapping(value = "/my-info")
    public ResponseEntity<SellerInfoResponse> getMyInfo(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(gordaService.getMyInfo(token));
    }

    @GetMapping(value = "/order-items")
    public ResponseEntity<List<GordaItemResponse>> getMyOrders(
            @RequestParam String token,
            @RequestParam(required = false) String collectStartDate
    ) {
        return ResponseEntity.ok(gordaService.getMyOrders(token, collectStartDate));
    }
    @GetMapping(value = "/order-items/{id}")
    public ResponseEntity<OrderDto.Request.CollectCallback> getOrderById(
            @RequestParam String token,
            @RequestParam String orderItemId
    ) {
        return ResponseEntity.ok(gordaService.getOrderById(token, orderItemId));
    }

    @GetMapping(value = "/collect-order-items")
    public ResponseEntity<OrderJobDto.Request.CollectCallback> collectOrderItems(
            @RequestParam String token,
            @RequestParam long jobId,
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam(required = false) String latestCollectOrderAt
    ) {
        ShopAccountDto.Request request = new ShopAccountDto.Request(loginId,password);
        request.setLatestCollectOrderAt(latestCollectOrderAt);
        return ResponseEntity.ok(gordaService.collectOrderFromShopTest(token, jobId, request));
    }

//    @PostMapping(value = "/oauth/tokens")
//    public ResponseEntity<GordaSellerTokenDto.Response> login(
//            @RequestBody GordaSellerTokenDto.Request loginRequest
//    ){
//        return ResponseEntity.ok(gordaService.login(loginRequest));
//    }
}
