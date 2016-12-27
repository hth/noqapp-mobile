package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.BizStoreEntity;
import com.token.domain.TokenQueueEntity;
import com.token.domain.json.JsonTokenState;
import com.token.repository.BizStoreManager;
import com.token.service.TokenQueueService;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenMobileService.class);

    private BizStoreManager bizStoreManager;
    private TokenQueueService tokenService;

    @Autowired
    public TokenMobileService(BizStoreManager bizStoreManager, TokenQueueService tokenService) {
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
        TokenQueueEntity tokenQueue = tokenService.findByCodeQR(codeQR);

        LOG.info("bizStore={} tokenQueue={}", bizStore.getBizName(), tokenQueue.getCurrentlyServing());
        return new JsonTokenState(bizStore.getCodeQR())
                .setBusinessName(bizStore.getBizName().getBusinessName())
                .setDisplayName(bizStore.getDisplayName())
                .setStoreAddress(bizStore.getAddress())
                .setStorePhone(bizStore.getPhoneFormatted())
                .setTokenAvailableFrom(bizStore.getTokenAvailableFrom())
                .setStartHour(bizStore.getStartHour())
                .setEndHour(bizStore.getEndHour())
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setLastNumber(tokenQueue.getLastNumber())
                .setCloseQueue(tokenQueue.isCloseQueue());
    }
}
