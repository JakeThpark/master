package com.wanpan.app.service.feelway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanpan.app.dto.job.qna.ShopQnaJobDto;
import com.wanpan.app.service.ShopAccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@Slf4j
@AllArgsConstructor
public class FeelwayTestEngineService {
    private final FeelwayService feelwayService;
    private final ShopAccountService shopAccountService;

    @Autowired
    @Qualifier("camelObjectMapper")
    private final ObjectMapper camelObjectMapper;

    public String collectQna(long shopAccountId, ShopQnaJobDto.QuestionStatus questionStatus, String askId) throws IOException, GeneralSecurityException {
        log.info("Call collectQna");
        //세션을 가지고 쿠키값 추출 파싱
        String token = shopAccountService.getTokenByShopAccountId(shopAccountId);
        log.info("------feelway token:{}",token);

        return camelObjectMapper.writeValueAsString(feelwayService.collectQna(token, questionStatus, askId));
    }
}
