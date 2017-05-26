package com.noqapp.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.service.BizService;
import com.noqapp.service.TokenQueueService;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenQueueMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueMobileService.class);

    private TokenQueueService tokenQueueService;
    private BizService bizService;
    private TokenQueueManager tokenQueueManager;

    @Autowired
    public TokenQueueMobileService(TokenQueueService tokenQueueService, BizService bizService, TokenQueueManager tokenQueueManager) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
        this.tokenQueueManager = tokenQueueManager;
    }

    public JsonQueue findTokenState(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        TokenQueueEntity tokenQueue = findByCodeQR(codeQR);

        LOG.info("bizStore={} tokenQueue={}", bizStore.getBizName(), tokenQueue.getCurrentlyServing());
        return new JsonQueue(bizStore.getCodeQR())
                .setBusinessName(bizStore.getBizName().getBusinessName())
                .setDisplayName(bizStore.getDisplayName())
                .setStoreAddress(bizStore.getAddress())
                .setCountryShortName(bizStore.getCountryShortName())
                .setStorePhone(bizStore.getPhoneFormatted())
                .setTokenAvailableFrom(bizStore.getTokenAvailableFrom())
                .setStartHour(bizStore.getStartHour())
                .setTokenNotAvailableFrom(bizStore.getTokenNotAvailableFrom())
                .setEndHour(bizStore.getEndHour())
                .setTopic(bizStore.getTopic())
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setLastNumber(tokenQueue.getLastNumber())
                .setQueueStatus(tokenQueue.getQueueStatus())
                .setCreated(tokenQueue.getCreated());
    }

    public JsonToken joinQueue(String codeQR, String did, String rid) {
        LOG.info("joinQueue codeQR={} did={} rid={}", codeQR, did, rid);
        return tokenQueueService.getNextToken(codeQR, did, rid);
    }

    public JsonResponse abortQueue(String codeQR, String did, String rid) {
        LOG.info("abortQueue codeQR={} did={} rid={}", codeQR, did, rid);
        return tokenQueueService.abortQueue(codeQR, did, rid);
    }

    public BizService getBizService() {
        return bizService;
    }

    JsonToken updateServing(String codeQR, QueueStatusEnum queueStatus, int serving) {
        return tokenQueueService.updateServing(codeQR, queueStatus, serving);
    }

    TokenQueueEntity findByCodeQR(String codeQR) {
        return tokenQueueManager.findByCodeQR(codeQR);
    }

    void changeQueueStatus(String codeQR, QueueStatusEnum queueStatus) {
        tokenQueueManager.changeQueueStatus(codeQR, queueStatus);
    }
}
