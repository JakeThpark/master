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
        //????????? ????????? ???????????? 1?????? Job ???????????? ????????????.(READY??? IN_PROGRESS ????????? ???????????? ?????? - ??????????????? ????????? READY??? ???????????? ?????????)
        List<Job> readyJobList = jobRepository.findByExecuteStatusGroupByShopAccountIdAndJobType(Arrays.asList(Job.ExcuteStatus.READY, Job.ExcuteStatus.IN_PROGRESS))
                .stream()
                .filter(job -> job.getExecuteStatus() == Job.ExcuteStatus.READY)
                .collect(Collectors.toList());
//        log.info("getJobList Size:{}",readyJobList.size());

        //TODO: ????????? Async ????????? ??????????????? ????????? ????????? ????????? ???????????? ????????? ????????? thread?????? ?????? ??? ?????? ??????
        for(Job job : readyJobList){
            log.info("Start job - jobId: {}",job.getId());
            //?????? Job??? ?????? ??????????????? ????????? ????????????.
            job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
            jobRepository.save(job);
        }
        divideJob(readyJobList);
    }

    /*
     * Job??? ???????????? ??? task?????? ????????????.
     */
    public void divideJobById(Long jobId) throws JsonProcessingException {
        //?????? ?????? ???????????? ????????????.
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

            //?????? Job??? ?????? ??????????????? ????????? ????????????.
            job.setExecuteStatus(Job.ExcuteStatus.IN_PROGRESS);
            jobRepository.save(job);

            switch (job.getJobType()){
                case WRITE: //?????? ?????? Job
                    log.debug("WRITE JOB REQUEST!!");
                    String loginId = "";
                    //?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        OnlineSaleDto onLineSaleDto = camelObjectMapper.readValue(job.getRequestData(), OnlineSaleDto.class);
                        if("FEELWAY".equals(onLineSaleDto.getShopSale().getShopAccount().getShopType())){
                            loginId = onLineSaleDto.getShopSale().getShopAccount().getLoginId();
                            log.info("===============Main processingSaleList:{}, job.getId():{}",processingSaleList, job.getId());
                            if(!processingSaleList.contains(loginId)){
                                log.info("===============FEELWAY IS NOT USE:{}, job:{}",loginId,job.getId());
                                //?????? ????????? ???????????? ?????????, ?????? ?????? ??????????????? ????????????.
                                processingSaleList.add(loginId);
                                postSaleToShopTaskService.postSaleToShopByJobRequest(job, onLineSaleDto);
                                log.info("===============Main after processingSaleList:{}, job.getId():{}",processingSaleList, job.getId());
                            }else{
                                log.info("===============FEELWAY IS USE:{}, job:{}",loginId,job.getId());
                                //?????? ????????? ????????? ?????????????????? ???????????? ?????? ???????????? ????????????.
                                //?????? Job??? ?????? ??????????????? ????????? ????????????.
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
                        //?????? case??? error?????? ?????? ?????? ??????
                        processingSaleList.remove(loginId);
                    }
                    break;
                case UPDATE: //?????? ?????? ??????
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
                case COLLECT_ORDER: //?????? ??????
                    log.debug("COLLECT_ORDER JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        OrderJobDto.Request.CollectJob collectOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.CollectJob.class);
                        collectOrderFromShopTaskService.collectOrderFromShopByJobRequest(job, collectOrderDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case UPDATE_ORDER: //?????? ??????
                    log.debug("UPDATE_ORDER JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        OrderJobDto.Request.UpdateJob updateOrderDto = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.UpdateJob.class);
                        updateOrderFromShopTaskService.updateOrderFromShopByJobRequest(job, updateOrderDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case POST_ORDER_CONVERSATION: //???????????? ?????? ??????
                    log.debug("POST_ORDER_CONVERSATION JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        OrderJobDto.Request.PostConversationJob postConversationJob = camelObjectMapper.readValue(job.getRequestData(), OrderJobDto.Request.PostConversationJob.class);
                        postOrderConversationToShopTaskService.postOrderConversationToShopByJobRequest(job, postConversationJob);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case COLLECT_ORDER_CONVERSATION: //??????????????? ?????? ??????
                    log.debug("COLLECT_ORDER_CONVERSATION JOB REQUEST!!");
                    //???????????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        OrderBaseConversationJobDto.Request.CollectJob collectOrderConversationJobDto = camelObjectMapper.readValue(job.getRequestData(), OrderBaseConversationJobDto.Request.CollectJob.class);
                        collectOrderBaseConversationFromShopTaskService.collectOrderBaseConversationFromShopByJobRequest(job, collectOrderConversationJobDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case COLLECT_QNA: //QnA??? ?????? ??????
                    log.debug("COLLECT_QNA JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        ShopQnaJobDto.Request.CollectJob collectQnADto = camelObjectMapper.readValue(job.getRequestData(), ShopQnaJobDto.Request.CollectJob.class);
                        collectQnaFromShopTaskService.collectQnAFromShopByJobRequest(job, collectQnADto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case POST_QNA_ANSWER: //QnA ?????? ??????
                    log.debug("POST_QNA_ANSWER JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        ShopQnaJobDto.Request.PostJob postQnaJobDto = camelObjectMapper.readValue(job.getRequestData(), ShopQnaJobDto.Request.PostJob.class);
                        postQnaToShopTaskService.postQnaToShopByJobRequest(job, postQnaJobDto);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case UPDATE_SALE_STATUS: //????????? ?????? ??????
                    log.debug("UPDATE_SALE_STATUS JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
                    try {
                        ShopSaleJobDto.Request.UpdateSaleStatusJob updateSaleStatusJob = camelObjectMapper.readValue(job.getRequestData(), ShopSaleJobDto.Request.UpdateSaleStatusJob.class);
                        updateSaleStatusToShopTaskService.updateSaleStatusToShopByJobRequest(job, updateSaleStatusJob);
                    }catch(JsonProcessingException e){
                        log.error("JsonProcessingException", e);
                    }catch(Exception e){
                        log.error("Exception", e);
                    }
                    break;
                case DELETE_SALE: //????????? ?????? ??????
                    log.debug("DELETE_SALE JOB REQUEST!!");
                    //?????? ?????? Job??? ?????? ???????????? Json??? ????????????.
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
