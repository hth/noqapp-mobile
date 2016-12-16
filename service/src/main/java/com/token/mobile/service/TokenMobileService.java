package com.token.mobile.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.BizStoreEntity;
import com.token.domain.TokenEntity;
import com.token.domain.json.JsonTokenState;
import com.token.repository.BizStoreManager;
import com.token.service.TokenService;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenMobileService {

    private BizStoreManager bizStoreManager;
    private TokenService tokenService;

    @Autowired
    public TokenMobileService(BizStoreManager bizStoreManager, TokenService tokenService) {
        this.bizStoreManager = bizStoreManager;
        this.tokenService = tokenService;
    }

    private BizStoreEntity findByCodeQR(String codeQR) {
        return bizStoreManager.findByCodeQR(codeQR);
    }

    public boolean isValidCodeQR(String codeQR) {
        return bizStoreManager.isValidCodeQR(codeQR);
    }

    public JsonTokenState findTokenState(String codeQR) {
        BizStoreEntity bizStore = findByCodeQR(codeQR);
        TokenEntity token = tokenService.findByCodeQR(codeQR);

        return new JsonTokenState(bizStore.getCodeQR())
                .setBusinessName(bizStore.getBizName().getBusinessName())
                .setDisplayName(bizStore.getDisplayName())
                .setStoreAddress(bizStore.getAddress())
                .setStorePhone(bizStore.getPhoneFormatted())
                .setTokenAvailableSince(bizStore.getTokenAvailableSince())
                .setStartHour(bizStore.getStartHour())
                .setEndHour(bizStore.getEndHour())
                .setServingNumber(token.getCurrentlyServing())
                .setLastNumber(token.getLastNumber())
                .setCloseQueue(token.isCloseQueue());
    }
}
