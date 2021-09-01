package com.wanpan.app.service.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.dto.job.OnlineSaleDto;
import com.wanpan.app.dto.job.ShopSaleJobDto;
import com.wanpan.app.dto.job.order.OrderBaseConversationJobDto;
import com.wanpan.app.dto.job.order.OrderJobDto;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.entity.Job;
import com.wanpan.app.entity.Shop;
import com.wanpan.app.repository.JobRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class JobService {
    public static List<String> processingSaleList = new ArrayList<>();
    private final JobRepository jobRepository;
    private final PostSaleToShopTaskService postSaleToShopTaskService;
    private final UpdateSaleToShopTaskService updateProductToShopByJobRequest;
    private final CollectQnaFromShopTaskService collectQnaFromShopTaskService;
    private final CollectOrderFromShopTaskService collectOrderFromShopTaskService;
    private final UpdateOrderFromShopTaskService updateOrderFromShopTaskService;
    private final UpdateSaleStatusToShopTaskService updateSaleStatusToShopTaskService;
    private final DeleteSaleToShopTaskService deleteSaleToShopTaskService;

    private final PostQnaToShopTaskService postQnaToShopTaskService;
    private final PostOrderConversationToShopTaskService postOrderConversationToShopTaskService;
    private final CollectOrderBaseConversationFromShopTaskService collectOrderBaseConversationFromShopTaskService;

    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;

    public void processJob() throws JsonProcessingException {
        //각각의 쇼핑몰 계정별로 1개씩 Job 데이타를 가져온다.(READY와 IN_PROGRESS 둘다를 가져오는 이유 - 진행중인게 있을때 READY를 가져오면 안된다)
        List<Job> readyJobList = jobRepository.findByExecuteStatusGroupByShopAccountIdAndJobType(Arrays.asList(Job.ExcuteStatus.READY, Job.ExcuteStatus.IN_PROGRESS))
                .stream()
                .filter(job -> job.getExecuteStatus() == Job.ExcuteStatus.READY)
                .collect(Collectors.toList());
//        log.info("getJobList Size:{}",readyJobList.size());

        //TODO: 계정별 Async 처리를 수행하는데 쇼핑몰 로그인 계정을 가져오는 부분이 두개의 thread에서 접근 할 수도 있음
        for(Job job : readyJobList){
            log.info("Start job - jobId: {}",job.getId());
            //해당 Job에 대해 작업중으로 상태를 변경한다.
            job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
            jobRepository.save(job);
        }
        divideJob(readyJobList);
    }

    /*
     * Job을 가져와서 각 task별로 분할한다.
     */
    public void divideJobById(Long jobId) throws JsonProcessingException {
        //작업 요청 리스트를 가져온다.
        List<Job> readyWriteJobList = null;
        if(ObjectUtils.isEmpty(jobId)){
            readyWriteJobList = jobRepository.findByExecuteStatus(Job.ExcuteStatus.READY);
        }else{
            readyWriteJobList = Arrays.asList(jobRepository.findById(jobId).orElseThrow());
        }
        divideJob(readyWriteJobList);
    }

    @Transactional
    public void divideJob(List<Job> readyWriteJobList) {

        for(Job job : readyWriteJobList){
            log.info("Start job - jobId: {}",job.getId());

            //해당 Job에 대해 작업중으로 상태를 변경한다.
            job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
            jobRepository.save(job);

            switch (job.getJobType()){
                case WRITE: //상품 등록 Job
                    log.debug("WRITE JOB REQUEST!!");
                    String loginId = "";
                    //등록 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        OnlineSaleDto onLineSaleDto = camelObjectMapper.readValue(job.getRequestData(), OnlineSaleDto.class);
                        if("FEELWAY".equals(onLineSaleDto.getShopSale().getShopAccount().getShopType())){
                            loginId = onLineSaleDto.getShopSale().getShopAccount().getLoginId();
                            log.info("===============Main processingSaleList:{}, job.getId():{}",processingSaleList, job.getId());
                            if(!processingSaleList.contains(loginId)){
                                log.info("===============FEELWAY IS NOT USE:{}, job:{}",loginId,job.getId());
                                //해당 계정이 사용중이 아닐때, 해당 계정 사용중으로 입력한다.
                                processingSaleList.add(loginId);
                                postSaleToShopTaskService.postSaleToShopByJobRequest(job, onLineSaleDto);
                                log.info("===============Main after processingSaleList:{}, job.getId():{}",processingSaleList, job.getId());
                            }else{
                                log.info("===============FEELWAY IS USE:{}, job:{}",loginId,job.getId());
                                //해당 계정의 등록을 사용중이므로 원복하고 다음 스케쥴에 실행한다.
                                //해당 Job에 대해 작업중으로 상태를 변경한다.
                                job.setExecuteStatus(Job.ExcuteStatus.READY);
                                jobRepository.save(job);
                            }
                        }else{
                            postSaleToShopTaskService.postSaleToShopByJobRequest(job, onLineSaleDto);
                        }
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                        processingSaleList.remove(loginId);
                    }catch(Exception e){
                        log.error("Exception", e);
                        //특이 case로 error시에 작업 목록 삭제
                        processingSaleList.remove(loginId);
                    }
                    break;
                case UPDATE: //상품 판매 수정
                    log.debug("UPDATE JOB REQUEST!!");
                    try {
                        OnlineSaleDto onLineSaleDto = camelObjectMapper.readValue(job.getRequestData(), OnlineSaleDto.class);
                        updateProductToShopByJobRequest.updateSaleToShopByJobRequest(job, onLineSaleDto);
                    } catch (IOException e) {
                        log.error("IOException", e);
                    } catch(Exception e) {
                        log.error("Exception", e);
                    }
                    break;
                case COLLECT_ORDER: //주문 수집
                    log.debug("COLLECT_ORDER JOB REQUEST!!");
                    //주문 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        OrderJobDto.Request.CollectJob collectOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.CollectJob.class);
                        collectOrderFromShopTaskService.collectOrderFromShopByJobRequest(job, collectOrderDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case UPDATE_ORDER: //주문 수정
                    log.debug("UPDATE_ORDER JOB REQUEST!!");
                    //주문 수정 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        OrderJobDto.Request.UpdateJob updateOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.UpdateJob.class);
                        updateOrderFromShopTaskService.updateOrderFromShopByJobRequest(job, updateOrderDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case POST_ORDER_CONVERSATION: //주문대화 답변 등록
                    log.debug("POST_ORDER_CONVERSATION JOB REQUEST!!");
                    //문의 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        OrderJobDto.Request.PostConversationJob postConversationJob = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.PostConversationJob.class);
                        postOrderConversationToShopTaskService.postOrderConversationToShopByJobRequest(job, postConversationJob);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case COLLECT_ORDER_CONVERSATION: //주문대화에 대한 수집
                    log.debug("COLLECT_ORDER_CONVERSATION JOB REQUEST!!");
                    //주문대화 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        OrderBaseConversationJobDto.Request.CollectJob collectOrderConversationJobDto = camelObjectMapper.readValue(job.getRequestData(), OrderBaseConversationJobDto.Request.CollectJob.class);
                        collectOrderBaseConversationFromShopTaskService.collectOrderBaseConversationFromShopByJobRequest(job, collectOrderConversationJobDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case COLLECT_QNA: //QnA에 대한 수집
                    log.debug("COLLECT_QNA JOB REQUEST!!");
                    //문의 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        ShopQnaJobDto.Request.CollectJob collectQnADto = camelObjectMapper.readValue(job.getRequestData(), ShopQnaJobDto.Request.CollectJob.class);
                        collectQnaFromShopTaskService.collectQnAFromShopByJobRequest(job, collectQnADto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case POST_QNA_ANSWER: //QnA 답변 등록
                    log.debug("POST_QNA_ANSWER JOB REQUEST!!");
                    //문의 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        ShopQnaJobDto.Request.PostJob postQnaJobDto = camelObjectMapper.readValue(job.getRequestData(), ShopQnaJobDto.Request.PostJob.class);
                        postQnaToShopTaskService.postQnaToShopByJobRequest(job, postQnaJobDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case UPDATE_SALE_STATUS: //쇼핑몰 상태 변경
                    log.debug("UPDATE_SALE_STATUS JOB REQUEST!!");
                    //문의 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob = camelObjectMapper.readValue(job.getRequestData(), ShopSaleJobDto.Request.UpdateSaleStatusJob.class);
                        updateSaleStatusToShopTaskService.updateSaleStatusToShopByJobRequest(job, updateSaleStatusJob);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case DELETE_SALE: //쇼핑몰 상태 변경
                    log.debug("DELETE_SALE JOB REQUEST!!");
                    //문의 수집 Job에 대한 작업요청 Json을 파싱한다.
                    try {
                        ShopSaleJobDto.Request.DeleteSaleJob deleteSaleJob = camelObjectMapper.readValue(job.getRequestData(), ShopSaleJobDto.Request.DeleteSaleJob.class);
                        deleteSaleToShopTaskService.deleteSaleToShopByJobRequest(job, deleteSaleJob);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                default:
                    break;
            }
        }
    }


}
