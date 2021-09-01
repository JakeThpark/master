package com.wanpan.app.controller;

import com.wanpan.app.service.ShopNoticeService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping({"/shop-notice"})
@AllArgsConstructor
public class ShopNoticeController {
    private final ShopNoticeService shopNoticeService;
    /*
     * 샵 공지사항 수집 서비스
     */
    @GetMapping(value = "/collect")
    @ApiOperation(value="샵 공지사항 수집 저장", notes = "샵 공지사항 수집 저장")
    public ResponseEntity<Resource> collectShopNotice() throws IOException {
        shopNoticeService.parseFeelwayNotice();
        log.info("==========================Feelway End========================");
        shopNoticeService.parseMustitNotice();
        log.info("==========================Mustit End========================");
        shopNoticeService.parseReebonzNotice();
        log.info("==========================Reebonz End========================");
        return ResponseEntity.ok().build();
    }
}
