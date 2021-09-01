package com.wanpan.app.service;

import com.wanpan.app.dto.ShopAccountDto;
import com.wanpan.app.entity.Shop;
import com.wanpan.app.entity.ShopAccount;
import com.wanpan.app.entity.ShopAccountToken;
import com.wanpan.app.repository.ShopAccountRepository;
import com.wanpan.app.repository.ShopAccountTokenRepository;
import com.wanpan.app.repository.ShopRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

/*
 * Shop Account 관련한 공통 서비스
 */

@Service
@Slf4j
@AllArgsConstructor
public class ShopAccountService {
    private ShopServiceFactory shopServiceFactory;
    private ShopAccountPasswordService shopAccountPasswordService;

    private ShopAccountTokenRepository shopAccountTokenRepository;
    private final ShopRepository shopRepository;
    private final ShopAccountRepository shopAccountRepository;

    public ShopAccountDto.Response shopSignInCheck(
            String loginId, String password,
            String type
    ) throws IOException {
        ShopAccountDto.Response shopAccountResponseDto =
                new ShopAccountDto.Response(loginId, password);

        ShopService shopService = shopServiceFactory.getShopService(type);
        return shopService.checkSignIn(loginId, password, shopAccountResponseDto);
    }

    /*
     * 토큰을 받아오는 default 메소드
     */
    public String getToken(String shopType, String accountId, String password)
            throws GeneralSecurityException, IOException {

        return getToken(shopType, accountId, password, ShopAccountToken.Type.SESSION);
    }

    public String getToken(ShopAccountDto.Request shopAccountDto) throws GeneralSecurityException, IOException {
        return getToken(shopAccountDto.getShopType(), shopAccountDto.getLoginId(), shopAccountDto.getPassword());
    }

    /*
     * 로그인을 멀티로 할 경우 토큰 타입에 따라서 토큰을 받아오는 메소드
     */
    public String getToken(String shopType, String accountId, String password, ShopAccountToken.Type tokenType)
            throws GeneralSecurityException, IOException {
//        Optional<ShopAccountToken> jobShopAccountToken = shopAccountTokenRepository.findByShop_TypeAndAccountId(shopType, accountId);
        Optional<ShopAccountToken> jobShopAccountToken = shopAccountTokenRepository.findByShopIdAndAccountIdAndType(shopType, accountId, tokenType);

        ShopService shopService = shopServiceFactory.getShopService(shopType);

        //저장된 토큰이 없을 경우 생성
        if (jobShopAccountToken.isEmpty()) {
            return createToken(shopType, accountId, password, tokenType);
        }

        //저장된 토큰인 경우 유효성 검사를 실시한다.
        boolean isAbleToken = jobShopAccountToken
                .map(j -> shopService.isKeepSignIn(j.getToken(), j.getAccountId(), tokenType))
                .orElse(false);

        if (isAbleToken) {
            return jobShopAccountToken.get().getToken();
        }

        return updateToken(jobShopAccountToken.get(), password, tokenType);
    }

    private String createToken(String type, String accountId, String password, ShopAccountToken.Type tokenType)
            throws GeneralSecurityException, IOException {
        ShopService shopService = shopServiceFactory.getShopService(type);
        String decryptedPassword = shopAccountPasswordService.decryptPassword(password);

        String token = shopService.getToken(accountId, decryptedPassword, tokenType);
        Shop shop = shopRepository.findById(type);
        ShopAccountToken shopAccountToken = new ShopAccountToken();
        shopAccountToken.setShopId(shop.getId());
        shopAccountToken.setAccountId(accountId);
        shopAccountToken.setToken(token);
        shopAccountToken.setType(tokenType);
        shopAccountTokenRepository.save(shopAccountToken);

        return token;
    }

    private String updateToken(ShopAccountToken shopAccountToken, String password, ShopAccountToken.Type tokenType)
            throws GeneralSecurityException, IOException {
        ShopService shopService = shopServiceFactory.getShopService(shopAccountToken.getShopId());
        String decryptedPassword = shopAccountPasswordService.decryptPassword(password);

        String token = shopService.getToken(shopAccountToken.getAccountId(), decryptedPassword, tokenType);

        shopAccountToken.setToken(token);
        shopAccountTokenRepository.save(shopAccountToken);

        return token;
    }

    public String getTokenByShopAccountId(final long shopAccountId)
            throws GeneralSecurityException, IOException {
        Optional<ShopAccount> shopAccount = shopAccountRepository.findById(shopAccountId);
        if(shopAccount.isEmpty()){
            return null;
        }
        log.info("shopAccount:{}",shopAccount);
        return getToken(shopAccount.get().getShop().getId(), shopAccount.get().getAccountId(), shopAccount.get().getPassword());
    }

    public String getTokenByShopAccount(final ShopAccount shopAccount)
            throws GeneralSecurityException, IOException {
        log.info("shopAccount:{}",shopAccount);
        return getToken(shopAccount.getShop().getId(), shopAccount.getAccountId(), shopAccount.getPassword());
    }
}
