package com.wanpan.app.controller;

import com.wanpan.app.service.TwoCaptchaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping({"/captcha"})
@AllArgsConstructor
public class TwoCaptchaController {
    private final TwoCaptchaService twoCaptchaService;

    @GetMapping
    public ResponseEntity<String> getCode(
            @RequestParam(value = "image-url") String imageUrl)
            throws IOException, InterruptedException {

        return ResponseEntity.ok(twoCaptchaService.getCaptcha(imageUrl));
    }
}
