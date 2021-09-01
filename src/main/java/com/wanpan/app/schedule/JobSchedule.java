package com.wanpan.app.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wanpan.app.service.job.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile(value = {"dev", "live"})
public class JobSchedule {

    @Autowired
    private JobService jobService;

    @Scheduled(fixedDelay = 1000)
    public void jobExecuteService() throws InterruptedException, JsonProcessingException {
        jobService.processJob();
    }
}
