package com.wanpan.app.config.gateway;

import com.wanpan.app.config.FeignConfiguration;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "internal", url = "${internal.client.url}", configuration = FeignConfiguration.class, decode404 = true) //http://localhost:8080/v1"
public interface InternalClient {

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/shop-sale/update")
    ResponseEntity<String> postAndUpdateShopSaleCallback(
            @RequestBody RegisterDto.Response shopSaleResult
    );

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/shop-qna/collect")
    ResponseEntity<String> collectQnaCallback(
            @RequestBody ShopQnaJobDto.Request.CollectCallback collectShopQnaListCallback
    );

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/shop-qna/post-answer")
    ResponseEntity<String> postAnswerCallback(
            @RequestBody ShopQnaJobDto.Request.PostCallback postAnswerCallback
    );

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/order/collect")
    ResponseEntity<String> collectOrderCallback(
            @RequestBody OrderJobDto.Request.CollectCallback collectOrderListCallback
    );

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/order/update")
    ResponseEntity<String> updateOrderCallback(
            @RequestBody OrderJobDto.Request.UpdateCallback updateOrderListCallback
    );

    @Headers("Content-Type: application/json")
    @PutMapping(value = "/internal/order-conversation/update")
    ResponseEntity<String> collectOrderConversationCallback(
            @RequestBody OrderBaseConversationJobDto.Request.CollectCallback collectOrderConversationCallback
    );

}
