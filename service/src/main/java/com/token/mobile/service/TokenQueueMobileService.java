package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.BizStoreEntity;
import com.token.domain.TokenQueueEntity;
import com.token.domain.json.JsonQueue;
import com.token.domain.json.JsonResponse;
import com.token.domain.json.JsonToken;
import com.token.domain.types.QueueStatusEnum;
import com.token.service.BizService;
import com.token.service.TokenQueueService;

/**
 * User: hitender
 * Date: 11/17/16 3:10 PM
 */
@Service
public class TokenQueueMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenQueueMobileService.class);

    private TokenQueueService tokenQueueService;
    private BizService bizService;

    @Autowired
    public TokenQueueMobileService(TokenQueueService tokenQueueService, BizService bizService) {
        this.tokenQueueService = tokenQueueService;
        this.bizService = bizService;
    }

    public JsonQueue findTokenState(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        TokenQueueEntity tokenQueue = findByCodeQR(codeQR);

        LOG.info("bizStore={} tokenQueue={}", bizStore.getBizName(), tokenQueue.getCurrentlyServing());
        return new JsonQueue(bizStore.getCodeQR())
                .setBusinessName(bizStore.getBizName().getBusinessName())
                .setDisplayName(bizStore.getDisplayName())
                .setStoreAddress(bizStore.getAddress())
                .setStorePhone(bizStore.getPhoneFormatted())
                .setTokenAvailableFrom(bizStore.getTokenAvailableFrom())
                .setStartHour(bizStore.getStartHour())
                .setEndHour(bizStore.getEndHour())
                .setTopic(bizStore.getTopic())
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setLastNumber(tokenQueue.getLastNumber())
                .setQueueStatus(tokenQueue.isCloseQueue());
    }

    public JsonToken joinQueue(String codeQR, String did, String rid) {
        return tokenQueueService.getNextToken(codeQR, did, rid);
    }

    public JsonResponse abortQueue(String codeQR, String did, String rid) {
        return tokenQueueService.abortQueue(codeQR, did, rid);
    }

    public BizService getBizService() {
        return bizService;
    }

    public JsonToken updateServing(String codeQR, QueueStatusEnum queueStatus, int serving) {
        return tokenQueueService.updateServing(codeQR, queueStatus, serving);
    }

    public TokenQueueEntity findByCodeQR(String codeQR) {
        return tokenQueueService.findByCodeQR(codeQR);
    }
}
