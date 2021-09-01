package com.wanpan.app.controller;

import com.wanpan.app.service.job.JobService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping({"/job"})
@AllArgsConstructor
public class JobController {
    private JobService jobService;
    /*
     * 리본즈 브랜드 목록
     */
    @GetMapping(value = "/write-to-shop")
    @ApiOperation(value="판매 등록 요청 Job 처리", notes = "쇼핑몰 판매 등록 요청 Job 처리")
    public ResponseEntity<Resource> writeToShop(
            @RequestParam(value="job-id", required = false) Long jobId
    ) throws IOException {
        jobService.divideJobById(jobId);
        return ResponseEntity.ok().build();
    }


}
