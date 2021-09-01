package com.wanpan.app.config.gateway;

import com.wanpan.app.config.FeignConfiguration;
import com.wanpan.app.dto.reebonz.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "reebonz", url = "${external.reebonz.url}", configuration = FeignConfiguration.class, decode404 = true)
public interface ReebonzClient {

    @GetMapping("/marketplace/product/brands.json")
    ReebonzBaseResponse<BrandListResponse> getBrands(
            @RequestHeader("Authorization") String token,
            @RequestParam("search_name") String searchName
    );

    @GetMapping("/marketplace/product/categories.json")
    ResponseEntity<ReebonzBaseResponse<Categories>> getCategories(
            @RequestHeader("Authorization") String token,
            @RequestParam("new_category") boolean newCategory,
            @RequestParam("parent_id") int parentId
    );

    @GetMapping("/marketplace/product/product_meta_info.json")
    ResponseEntity<ReebonzBaseResponse<ProductMetaInfoResponse>> getProductMetaInfoByCode(
            @RequestHeader("Authorization") String token,
            @RequestParam("code") String skuCode
    );

    @PostMapping("/marketplace/order/deliveries/create_or_update.json")
    ResponseEntity<ReebonzBaseResponse<DeliveryRequest>> createOrUpdateDelivery(
            @RequestHeader("Authorization") String token,
            @RequestBody DeliveryRequest deliveryRequest
    );

    @PostMapping("/token")
    ResponseEntity<LoginResponse> login(
            @RequestParam("grant_type") String grantType,
            @RequestParam("username") String username,
            @RequestParam("password") String password
    );

    /**
     * post 후에 등록된 ID가 리턴된다.
     * @param token
     * @param reebonzApiProductCreate
     * @return
     */
    @PostMapping("/marketplace/product/products.json")
    ResponseEntity<ReebonzApiProductCreate.Response> postProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody ReebonzApiProductCreate reebonzApiProductCreate
    );

    /**
     * 판매내용 수정
     * @param token
     * @param reebonzProductUpdate
     * @return
     */
    @PutMapping("/marketplace/product/products/{id}.json")
    ResponseEntity<ReebonzBaseResponse<ProductResponse>> updateProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String postId,
            @RequestBody ReebonzProductUpdate reebonzProductUpdate
    );

    /**
     * 주문목록 조회
     * @param token
     * @return
     */
    @GetMapping("/marketplace/order/ordered_items.json")
    ResponseEntity<ReebonzBaseResponse<ReebonzOrderDto.Response.Collect>> getOrderList(
            @RequestHeader("Authorization") String token,
            @RequestParam("per_page") String perPage,
            @RequestParam("current_page") String currentPage,
            @RequestParam("order_status") String orderStatus,
            @RequestParam("delivery_status") String deliveryStatus,
            @RequestParam("ordered_at") String orderedAt,
            @RequestParam("ordered_at_start") String orderedAtStart,
            @RequestParam("ordered_at_end") String orderedAtEnd,
            @RequestParam("filter_to_reebonz") String filterToReebonz
    );

}
