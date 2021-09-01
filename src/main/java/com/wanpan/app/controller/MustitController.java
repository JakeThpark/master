package com.wanpan.app.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.gateway.MustitClient;
import com.wanpan.app.dto.BrandDto;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.mustit.MustitCategory;
import com.wanpan.app.entity.Job;
import com.wanpan.app.repository.JobRepository;
import com.wanpan.app.service.BrandService;
import com.wanpan.app.service.ShopAccountService;
import com.wanpan.app.service.ShopServiceFactory;
import com.wanpan.app.service.job.*;
import com.wanpan.app.service.mustit.MustitBrandService;
import com.wanpan.app.service.mustit.MustitCategoryService;
import com.wanpan.app.service.mustit.MustitService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.wanpan.app.entity.Job.JobType.*;

@RestController
@Slf4j
@RequestMapping({"/mustit"})
@AllArgsConstructor
public class MustitController {
    private final ShopAccountService shopAccountService;
    private final MustitBrandService mustitBrandService;
    private final MustitCategoryService mustitCategoryService;
    private final BrandService brandService;
    private final ShopServiceFactory shopServiceFactory;
    private final MustitService mustitService;
    private final JobTaskService jobTaskService;
    private final JobRepository jobRepository;
    private final PostSaleToShopTaskService postSaleToShopTaskService;
    private final UpdateSaleToShopTaskService updateSaleToShopTaskService;
    private final CollectOrderFromShopTaskService collectOrderFromShopTaskService;
    private final UpdateOrderFromShopTaskService updateOrderFromShopTaskService;
    private final PostOrderConversationToShopTaskService postOrderConversationToShopTaskService;

    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;

    @Data
    @NoArgsConstructor
    public static class MustitJobBaseRequest {
        private long shopAccountId;
    }

    @GetMapping(value = "/brands")
    public ResponseEntity<List<BrandDto>> getBrandList(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId,
            @RequestParam(value="search-name", required = true) String searchName)
            throws IOException, GeneralSecurityException {
        String token = shopAccountService.getTokenByShopAccountId(shopAccountId);
        return ResponseEntity.ok(mustitBrandService.getBrandListBySearchName(token, searchName));
    }

    /**
     * 머스트잇 브랜드 리스트를 끌어와서 기준 테이블과 비교 후 매핑한다.
     */
    @PostMapping(value = "/brand-maps")
    public ResponseEntity<List<BrandDto>> mappingBrand(
            @RequestBody MustitController.MustitJobBaseRequest mustitJobBaseRequest) {
        return ResponseEntity.ok(brandService.mappingEachShopBrandByShopType("MUSTIT", mustitJobBaseRequest.getShopAccountId()));
    }

    @GetMapping(value = "/categories/notification-type")
    public ResponseEntity<Map<String,Long>> getSubCategory( )
            throws IOException {

        return ResponseEntity.ok(mustitCategoryService.getNotificationTypeFromJsonFile());
    }

    @GetMapping(value = "/categories/head")
    public ResponseEntity<List<MustitCategory>> getHeadCategoryList(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId,
            @RequestParam(value="category-head-type", required = true) MustitClient.HeadCategoryType categoryHeadType) throws IOException {

        return ResponseEntity.ok(mustitCategoryService.getHeadCategoryListByHeadType(shopAccountId, categoryHeadType));
    }

    @GetMapping(value = "/categories/sub")
    public ResponseEntity<List<MustitCategory>> getSubCategory(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId,
            @RequestParam(value="parent-category-id", required = true) String parentCategoryId,
            @RequestParam(value="category-head-type", required = true) MustitClient.HeadCategoryType categoryHeadType) throws IOException {

        return ResponseEntity.ok(mustitCategoryService.getSubCategoryListByHeadAndFlag(shopAccountId, parentCategoryId, categoryHeadType));
    }

    @GetMapping(value = "/categories/all")
    public ResponseEntity<List<MustitCategory>> getAllCategory(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId,
            @RequestParam(value="category-head-type", required = true) MustitClient.HeadCategoryType categoryHeadType) throws IOException {

        return ResponseEntity.ok(mustitCategoryService.getAllCategoryListByHeadType(shopAccountId, categoryHeadType));
    }

    @PostMapping(value = "/categories")
    public ResponseEntity<Boolean> createCategories(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId) throws IOException {

        return ResponseEntity.ok(mustitCategoryService.createCategories(shopAccountId));
    }

    @PostMapping(value = "/sale")
    public ResponseEntity<List<ListenableFuture<RegisterDto.Response>>> registerSale(
            @RequestParam String session
    ) {
        List<ListenableFuture<RegisterDto.Response>> responseList = new ArrayList<>();

        // 준비 Job 목록 구하기
        List<Job> readyJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);

        // 준비 Job별 처리
        for(Job job : readyJobList) {
            if (job.getJobType().equals(WRITE)) { // Job 타입이 "판매 등록"에 해당하는 것만 처리
                try {
                    OnlineSaleDto onlineSaleDto = camelObjectMapper.readValue(job.getRequestData(), OnlineSaleDto.class);
                    ShopSaleJobDto.Request.PostJob shopSaleDto = onlineSaleDto.getShopSale();
                    if (shopSaleDto.getShopAccount().getShopType().equals("MUSTIT")) { // 쇼핑몰 계정이 "머스트잇"인 것만 처리
                        //해당 Job에 대해 작업중으로 상태를 변경한다.
                        job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
                        jobRepository.save(job);

                        jobTaskService.checkAndChangeJobTask(
                                job.getId(), shopSaleDto.getId(), job.getJobType(), Job.ExcuteStatus.IN_PROGRESS, false, null);
                        ListenableFuture<RegisterDto.Response> response = mustitService.postSaleToShop(session, job, onlineSaleDto);
                        response.addCallback(postSaleToShopTaskService::resultPostSaleProcess, e -> log.error(e.getMessage(), e));

                        job.setExecuteStatus(Job.ExcuteStatus.END);
                        jobRepository.save(job);
                        responseList.add(response);
                    }
                } catch(JsonProcessingException jpe) {
                    log.error("JsonProcessingException", jpe);
                } catch (Exception e) {
                    log.error("Exception", e);
                }
            }
        }

        return ResponseEntity.ok(responseList);
    }

    @PostMapping(value = "/order-conversation")
    public ResponseEntity<String> sendOrderConversationMessage(
            @RequestParam String session
    ) {
        // 준비 Job 목록 구하기
        List<Job> readyJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);

        // 준비 Job별 처리
        for(Job job : readyJobList) {
            if (job.getJobType().equals(POST_ORDER_CONVERSATION)) { // Job 타입이 "주문대화 전송"에 해당하는 것만 처리
                try {
                    OrderJobDto.Request.PostConversationJob postConversationJobDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.PostConversationJob.class);
                    ShopAccountDto.Request shopAccountDto = postConversationJobDto.getShopAccount();
                    if ("MUSTIT".equals(postConversationJobDto.getShopAccount().getShopType())) { // 머스트잇 계정만 처리
                        //해당 Job에 대해 작업중으로 상태를 변경한다.
                        job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
                        jobRepository.save(job);

                        //DB에 상태 저장을 위해 row를 생성한다.(중복된 잡이 있는지 체크해야 한다. 요청ID와 JobID 복합)
                        jobTaskService.checkAndChangeJobTask(job.getId(), shopAccountDto.getRequestId(), job.getJobType(),Job.ExcuteStatus.IN_PROGRESS, false, null);

                        mustitService.postConversationMessageForOrderToShop(session, job.getId(), postConversationJobDto).addCallback(postOrderConversationToShopTaskService::resultPostConversationMessageProcess, (e) -> {
                            log.error(e.getMessage(), e);
                        });
                    }

                    //해당 잡내에 task에 대한 요청이 끝나면 Job를 종료한다.
                    job.setExecuteStatus(Job.ExcuteStatus.END);
                    jobRepository.save(job);
                }catch(JsonProcessingException e){
                    log.error("JsonProcessingException", e);
                }catch(Exception e){
                    log.error("Exception", e);
                }
            }
        }

        return ResponseEntity.ok().body(null);
    }

    @PutMapping(value = "/sale")
    public ResponseEntity<List<ListenableFuture<RegisterDto.Response>>> updateSale(
            @RequestParam String session
    ) {
        List<ListenableFuture<RegisterDto.Response>> responseList = new ArrayList<>();

        // 준비 Job 목록 구하기
        List<Job> readyJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);

        // 준비 Job별 처리
        for(Job job : readyJobList) {
            if (job.getJobType().equals(UPDATE)) { // Job 타입이 "판매 수정"에 해당하는 것만 처리
                try {
                    OnlineSaleDto onLineSaleDto = camelObjectMapper.readValue(job.getRequestData(), OnlineSaleDto.class);
                    ShopSaleJobDto.Request.PostJob shopSaleDto = onLineSaleDto.getShopSale();
                    if ("MUSTIT".equals(shopSaleDto.getShopAccount().getShopType())) { // 쇼핑몰 계정이 "머스트잇"인 것만 처리
                        //해당 Job에 대해 작업중으로 상태를 변경한다.
                        job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
                        jobRepository.save(job);

                        jobTaskService.checkAndChangeJobTask(
                                job.getId(), shopSaleDto.getId(), job.getJobType(), Job.ExcuteStatus.IN_PROGRESS, false, null);
                        ListenableFuture<RegisterDto.Response> response = mustitService.updateSaleToShop(session, job.getId(), onLineSaleDto);
                        response.addCallback(updateSaleToShopTaskService::resultUpdateProductProcess, e -> log.error(e.getMessage(), e));

                        job.setExecuteStatus(Job.ExcuteStatus.END);
                        jobRepository.save(job);
                        responseList.add(response);
                    }
                } catch (IOException e) {
                    log.error("IOException", e);
                } catch(Exception e) {
                    log.error("Exception", e);
                }
            }
        }

        return ResponseEntity.ok(responseList);
    }

    @PutMapping(value = "/orders")
    public ResponseEntity<String> updateOrder(
            @RequestParam String session
    ) {
        // 준비 Job 목록 구하기
        List<Job> readyJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);

        // 준비 Job별 처리
        for(Job job : readyJobList) {
            if (job.getJobType().equals(UPDATE_ORDER)) { // Job 타입이 "주문 수정"에 해당하는 것만 처리
                try {
                    OrderJobDto.Request.UpdateJob updateOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.UpdateJob.class);
                    ShopAccountDto.Request shopAccountDto = updateOrderDto.getShopAccount();
                    if ("MUSTIT".equals(updateOrderDto.getShopAccount().getShopType())) { // 머스트잇 계정만 처리
                        //해당 Job에 대해 작업중으로 상태를 변경한다.
                        job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
                        jobRepository.save(job);

                        //DB에 상태 저장을 위해 row를 생성한다.(중복된 잡이 있는지 체크해야 한다. 요청ID와 JobID 복합)
                        jobTaskService.checkAndChangeJobTask(job.getId(), shopAccountDto.getRequestId(), job.getJobType(),Job.ExcuteStatus.IN_PROGRESS, false, null);

                        mustitService.updateOrderToShop(session, job.getId(), updateOrderDto).addCallback(updateOrderFromShopTaskService::resultUpdateOrderProcess, (e) -> {
                            log.error(e.getMessage(), e);
                        });
                    }

                    //해당 잡내에 task에 대한 요청이 끝나면 Job를 종료한다.
                    job.setExecuteStatus(Job.ExcuteStatus.END);
                    jobRepository.save(job);
                }catch(JsonProcessingException e){
                    log.error("JsonProcessingException", e);
                }catch(Exception e){
                    log.error("Exception", e);
                }
            }
        }

        return ResponseEntity.ok().body(null);
    }

//    @DeleteMapping(value = "/sale")
//    public ResponseEntity<Boolean> deleteSale(
//            @RequestParam String saleId
//    ) {
//        ShopService shopService = shopServiceFactory.getShopService(Shop.Type.MUSTIT);
//        return ResponseEntity.ok(shopService.deleteShopSale(null, saleId));
//    }

    @GetMapping(value = "/categories/filter")
    public ResponseEntity<Boolean> getAllCategory(
            @RequestParam(value="shop-account-id", required = true) long shopAccountId) throws IOException {

        return ResponseEntity.ok(mustitCategoryService.getCategoryFilter(shopAccountId));
    }

    /**
     * 머스트잇 주문 수집
     */
    @GetMapping(value = "/orders")
    @ApiOperation(value="머스트잇 주문 수집", notes = "주문 수집")
    public ResponseEntity<String> getOrderList(
            @RequestParam String session
    ) {
        // 준비 Job 목록 구하기
        List<Job> readyJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);

        // 준비 Job별 처리
        for(Job job : readyJobList) {
            if (job.getJobType().equals(COLLECT_ORDER)) { // Job 타입이 "주문 수집"에 해당하는 것만 처리
                try {
                    OrderJobDto.Request.CollectJob collectOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.CollectJob.class);
                    for(ShopAccountDto.Request request : collectOrderDto.getShopAccounts()) {
                        if ("MUSTIT".equals(request.getShopType())) { // 머스트잇 계정만 처리
                            //해당 Job에 대해 작업중으로 상태를 변경한다.
                            job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
                            jobRepository.save(job);

                            //DB에 상태 저장을 위해 row를 생성한다.(중복된 잡이 있는지 체크해야 한다. 요청ID와 JobID 복합)
                            jobTaskService.checkAndChangeJobTask(job.getId(), request.getRequestId(), job.getJobType(),Job.ExcuteStatus.IN_PROGRESS, false, null);

                            mustitService.collectOrderFromShop(session, job.getId(), collectOrderDto.getOrderProcessStatus(), request).addCallback(collectOrderFromShopTaskService::resultCollectOrderProcess, (e) -> {
                                log.error(e.getMessage(), e);
                            });
                        }
                    }
                    //해당 잡내에 task에 대한 요청이 끝나면 Job를 종료한다.
                    job.setExecuteStatus(Job.ExcuteStatus.END);
                    jobRepository.save(job);
                }catch(JsonProcessingException e){
                    log.error("JsonProcessingException", e);
                }catch(Exception e){
                    log.error("Exception", e);
                }
            }
        }

        return ResponseEntity.ok().body(null);
    }


}
