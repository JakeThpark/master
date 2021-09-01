package com.wanpan.app.service.job;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.config.gateway.InternalClient;
import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.JobTask;
import com.wanpan.app.entity.ShopAccountToken;
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
 * QnA 수집 데이타를 위한 서비스
 */
@Service
@AllArgsConstructor
@Slf4j
public class CollectOrderBaseConversationFromShopTaskService {
    private final ShopServiceFactory shopServiceFactory;
    private final JobTaskService jobTaskService;
    private final JobRepository jobRepository;
    private final ShopAccountService shopAccountService;
    private final InternalClient internalClient;

    /*
     * ShopAccount 목록을 받아서 각각의 account에 대해서 주문대화를 수집한다.
     */
    public void collectOrderBaseConversationFromShopByJobRequest(Job job, OrderBaseConversationJobDto.Request.CollectJob collectOrderBaseConversationDto) throws IOException, GeneralSecurityException {
        log.debug("Call collectOrderBaseConversationFromShopByJobRequest - jobId:{}", job.getId());
        for(ShopAccountDto.Request request : collectOrderBaseConversationDto.getShopAccounts()){
            //DB에 상태 저장을 위해 row를 생성한다.(중복된 잡이 있는지 체크해야 한다. 요청ID와 JobID 복합)
            jobTaskService.checkAndChangeJobTask(job.getId(), request.getRequestId(), job.getJobType(),Job.ExcuteStatus.IN_PROGRESS, false, null);
            //Reebonz의 경우 수집은 SESSION타입이므로 동일하게 사용한다.
            String token = shopAccountService.getToken(request.getShopType(),request.getLoginId(), request.getPassword(), ShopAccountToken.Type.SESSION);
            //각 서비스 타입별로 맞는 등록 서비스를 호출한다. async를 호출하고 callback을 받아서 처리한다.
            ShopService shopService = shopServiceFactory.getShopService(request.getShopType());
            shopService.collectOrderConversationFromShop(token, job.getId(), collectOrderBaseConversationDto.getOrderConversationStatus(), request).addCallback(this::resultCollectOrderConversationProcess, (e) -> {
                log.error(e.getMessage(), e);

            });
        }
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
    public void resultCollectOrderConversationProcess(OrderBaseConversationJobDto.Request.CollectCallback result){
        //개별 jobtask에 대한상태를 결과에 따라 변환한다.
        JobTask jobTask = jobTaskService.checkAndChangeJobTask(
                result.getJobTaskResponseBaseDto().getJobId(),
                result.getJobTaskResponseBaseDto().getRequestId(),
                Job.JobType.COLLECT_ORDER_CONVERSATION,
                Job.ExcuteStatus.END,
                result.getJobTaskResponseBaseDto().isSuccessFlag(),
                result.getJobTaskResponseBaseDto().getMessage()
        );

        try {
            log.info(camelObjectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //Callback API를 호출한다.
        ResponseEntity<String> responseEntity = internalClient.collectOrderConversationCallback(result);
        log.info("{}", responseEntity);
        if(responseEntity.getStatusCode() == HttpStatus.OK){
            //콜백처리 완료시 해당 task를 삭제한다.
            jobTaskService.deleteJobTask(jobTask);
        }else{
            //TODO:콜백 처리가 안되었거나 실패시의 Process
            log.info("failed COLLECT_QNA Job Task Callback process - jobTask:{}", jobTask);
        }
    }
}
