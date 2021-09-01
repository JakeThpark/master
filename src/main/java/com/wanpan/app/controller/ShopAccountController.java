package com.wanpan.app.controller;

import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.service.ShopAccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@Slf4j
@RequestMapping({"/shops"})
@AllArgsConstructor
public class ShopAccountController {

    private final ShopAccountService shopAccountService;

    @GetMapping(value = "/check-login")
    public ResponseEntity<ShopAccountDto.Response> checkLogin(
            @RequestParam(value = "login-id") String accountId,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "shop-type") String type) throws IOException {

        return ResponseEntity.ok(shopAccountService.shopSignInCheck(accountId, password, type));
    }

    @GetMapping(value = "/token")
    public ResponseEntity<String> getToken(
            @RequestParam(value = "login-id") String accountId,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "shop-type") String type)
            throws GeneralSecurityException, IOException {


        return ResponseEntity.ok(shopAccountService.getToken(type, accountId, password));

    }
}
