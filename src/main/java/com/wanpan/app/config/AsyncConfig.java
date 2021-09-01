package com.wanpan.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;


/**
 * Created by taehyeong.park on 2017-12-05.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("aysnc-pool-");
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(100);
        threadPoolTaskExecutor.setQueueCapacity(50);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception message - ").append(ex.getMessage()).append("\n");
            sb.append("Method name -  ").append(method.getName()).append("\n");

            for (Object param : params) {
                sb.append("Parameter value - ").append(param).append("\n");
            }
            log.error(sb.toString(), ex);
        };
    }
}