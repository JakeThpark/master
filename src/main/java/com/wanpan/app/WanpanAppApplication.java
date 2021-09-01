package com.wanpan.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableJpaRepositories
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableAsync
public class WanpanAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WanpanAppApplication.class, args);
    }

}
