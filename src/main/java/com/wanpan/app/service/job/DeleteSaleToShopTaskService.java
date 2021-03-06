package com.wanpan.app.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.gateway.InternalClient;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.JobTask;
import com.wanpan.app.repository.JobRepository;
import com.wanpan.app.service.ShopAccountService;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.ShopServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteSaleToShopTaskService {
    private final ShopServiceFactory shopServiceFactory;
    private final ShopAccountService shopAccountService;
    private final JobTaskService jobTaskService;
    private final JobRepository jobRepository;
    private final InternalClient internalClient;

    public void deleteSaleToShopByJobRequest(Job job, ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob)
            throws GeneralSecurityException, IOException {
        log.debug("Call deleteSaleToShopByJobRequest - jobId:{}", job.getId());

        jobTaskService.checkAndChangeJobTask(job.getId(), deleteSaleJob.getId(), job.getJobType());

        log.debug("parsed deleteSaleJob : {}", deleteSaleJob);

        ShopAccountDto.Request shopAccountDto = deleteSaleJob.getShopAccount();
        String token = shopAccountService.getToken(shopAccountDto);
        ShopService shopService = shopServiceFactory.getShopService(shopAccountDto.getShopType());

        shopService.deleteShopSale(token, job.getId(), deleteSaleJob)
                .addCallback(this::resultDeleteProductProcess, e -> log.error(e.getMessage(), e));

        job.setExecuteStatus(Job.ExcuteStatus.END);
        jobRepository.save(job);
    }


    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;
    public void resultDeleteProductProcess(RegisterDto.Response result) {
        log.info("DeleteSaleToShopTaskService.resultDeleteProductProcess Job return: {}", result);

        //TODO:?????? ??????????????? ??????????????? ?????? ???????????? ??????.
        result.setRequestId(result.getShopSaleId());

        //?????? jobtask??? ??????????????? ????????? ?????? ????????????.
        JobTask jobTask = jobTaskService.checkAndChangeJobTask(
                result.getJobId(),
                result.getRequestId(),
                Job.JobType.DELETE_SALE,
                Job.ExcuteStatus.END,
                result.isSuccessFlag(),
                result.getMessage()
        );

        try {
            log.info(camelObjectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //Callback API??? ????????????.
        ResponseEntity<String> responseEntity = internalClient.postAndUpdateShopSaleCallback(result);
        log.info("{}", responseEntity);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            //???????????? ????????? ?????? task??? ????????????.
            jobTaskService.deleteJobTask(jobTask);
        } else {
            //TODO:?????? ????????? ??????????????? ???????????? Process
            log.info("failed WRITE Job Task Callback process - jobTask:{}", jobTask);
        }
    }
}
