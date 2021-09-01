package com.wanpan.app.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.gateway.InternalClient;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.RegisterDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.JobTask;
import com.wanpan.app.repository.JobRepository;
import com.wanpan.app.service.ShopAccountService;
import com.wanpan.app.service.ShopService;
import com.wanpan.app.service.ShopServiceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

/*
 * 쇼핑몰 판매 CRUD 관련 서비스
 */
@Service
@AllArgsConstructor
@Slf4j
public class PostSaleToShopTaskService {
    private final ShopServiceFactory shopServiceFactory;
    private final JobTaskService jobTaskService;
    private final JobRepository jobRepository;
    private final InternalClient internalClient;
    private final ShopAccountService shopAccountService;

    /**
     * 등록요청 Job에 대한 메인 처리 process
     *
     * @param onlineSaleDto Job요청시에 RequestData영역에 들어오는 OnlineSaleDto
     */
    public void postSaleToShopByJobRequest(Job job, OnlineSaleDto onlineSaleDto)
            throws IOException, GeneralSecurityException {
        log.debug("Call registerProductToShopByJobRequest - jobId:{}", job.getId());

        //DB에 상태 저장을 위해 row를 생성한다.(중복된 잡이 있는지 체크해야 한다. 요청ID와 JobID 복합)
        ShopSaleJobDto.Request.PostJob shopSaleDto = onlineSaleDto.getShopSale();
        jobTaskService
                .checkAndChangeJobTask(job.getId(), shopSaleDto.getId(), job.getJobType(), Job.ExcuteStatus.IN_PROGRESS,
                        false, null);

        log.debug("parsed shopSaleDto : {}", shopSaleDto);

        // 토큰 타입 기본값을 "SESSION"으로 하여 토큰을 구한다
        String token = shopAccountService
                .getToken(shopSaleDto.getShopAccount().getShopType(), shopSaleDto.getShopAccount().getLoginId(),
                        shopSaleDto.getShopAccount().getPassword());

        // 서비스 타입별로 맞는 등록 서비스를 호출한다.async를 호출하고 callback을 받아서 처리한다.
        ShopService shopService = shopServiceFactory.getShopService(shopSaleDto.getShopAccount().getShopType());
        shopService.postSaleToShop(token, job, onlineSaleDto)
                .addCallback(this::resultPostSaleProcess, e -> log.error(e.getMessage(), e));
        //해당 잡내에 task에 대한 요청이 끝나면 Job를 종료한다.
        job.setExecuteStatus(Job.ExcuteStatus.END);
        jobRepository.save(job);
    }

    /*
     * 각 서비스별 등록작업에 대해 비동기로 받아온 결과를 가지고 상태값을 변경한다.
     */
    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;
    public void resultPostSaleProcess(RegisterDto.Response result) {
        log.info("resultRegisterProcess Job return: {}", result);

        //TODO:해당 인터페이스 구현부에서 모두 수정해야 한다.
        result.setRequestId(result.getShopSaleId());

        //개별 jobtask에 대한상태를 결과에 따라 변환한다.
        JobTask jobTask = jobTaskService.checkAndChangeJobTask(
                result.getJobId(),
                result.getRequestId(),
                Job.JobType.WRITE,
                Job.ExcuteStatus.END,
                result.isSuccessFlag(),
                result.getMessage()
        );

        try {
            log.info(camelObjectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //Callback API를 호출한다.
        ResponseEntity<String> responseEntity = internalClient.postAndUpdateShopSaleCallback(result);
        log.info("{}", responseEntity);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            //콜백처리 완료시 해당 task를 삭제한다.
            jobTaskService.deleteJobTask(jobTask);
        } else {
            //TODO:콜백 처리가 안되었거나 실패시의 Process
            log.info("failed WRITE Job Task Callback process - jobTask:{}", jobTask);
        }
    }

}
