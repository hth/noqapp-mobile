package com.noqapp.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.repository.TokenQueueManager;
import com.noqapp.service.BizService;
import com.noqapp.service.TokenQueueService;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.TimeZone;

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
        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            StoreHourEntity storeHour = getStoreHours(codeQR, bizStore);
            TokenQueueEntity tokenQueue = findByCodeQR(codeQR);
            LOG.info("TokenState bizStore={} averageServiceTime={} tokenQueue={}",
                    bizStore.getBizName(),
                    bizStore.getAverageServiceTime(),
                    tokenQueue.getCurrentlyServing());

            return new JsonQueue(bizStore.getCodeQR())
                    .setBusinessName(bizStore.getBizName().getBusinessName())
                    .setDisplayName(bizStore.getDisplayName())
                    .setStoreAddress(bizStore.getAddress())
                    .setCountryShortName(bizStore.getCountryShortName())
                    .setStorePhone(bizStore.getPhoneFormatted())
                    .setRating(bizStore.getRating())
                    .setRatingCount(bizStore.getRatingCount())
                    .setAverageServiceTime(bizStore.getAverageServiceTime())
                    .setTokenAvailableFrom(storeHour.getTokenAvailableFrom())
                    .setStartHour(storeHour.getStartHour())
                    .setTokenNotAvailableFrom(storeHour.getTokenNotAvailableFrom())
                    .setEndHour(storeHour.getEndHour())
                    .setPreventJoining(storeHour.isPreventJoining())
                    .setDayClosed(storeHour.isDayClosed())
                    .setTopic(bizStore.getTopic())
                    .setCoordinate(bizStore.getCoordinate())
                    .setServingNumber(tokenQueue.getCurrentlyServing())
                    .setLastNumber(tokenQueue.getLastNumber())
                    .setQueueStatus(tokenQueue.getQueueStatus())
                    .setCreated(tokenQueue.getCreated())
                    .setRemoteJoinAvailable(bizStore.isRemoteJoin())
                    .setAllowLoggedInUser(bizStore.isAllowLoggedInUser());
        } catch (Exception e) {
            //TODO remove this catch
            LOG.error("Failed getting state codeQR={} reason={}", codeQR, e.getLocalizedMessage(), e);
            return null;
        }
    }

    //TODO instead send all the hours of the store and let App figure out which one to show.
    private StoreHourEntity getStoreHours(String codeQR, BizStoreEntity bizStore) {
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        LOG.debug("codeQR={} dayOfWeek={}", codeQR, dayOfWeek);

        StoreHourEntity storeHour = bizService.findStoreHour(bizStore.getId(), dayOfWeek);
        LOG.debug("StoreHour={}", storeHour);
        return storeHour;
    }

    public JsonToken joinQueue(String codeQR, String did, String qid) {
        LOG.info("joinQueue codeQR={} did={} rid={}", codeQR, did, qid);
        return tokenQueueService.getNextToken(codeQR, did, qid);
    }

    public JsonResponse abortQueue(String codeQR, String did, String qid) {
        LOG.info("abortQueue codeQR={} did={} rid={}", codeQR, did, qid);
        return tokenQueueService.abortQueue(codeQR, did, qid);
    }

    public BizService getBizService() {
        return bizService;
    }

    JsonToken updateServing(String codeQR, QueueStatusEnum queueStatus, int serving, String goTo) {
        return tokenQueueService.updateServing(codeQR, queueStatus, serving, goTo);
    }

    JsonToken updateThisServing(String codeQR, QueueStatusEnum queueStatus, int serving, String goTo) {
        return tokenQueueService.updateThisServing(codeQR, queueStatus, serving, goTo);
    }

    TokenQueueEntity findByCodeQR(String codeQR) {
        return tokenQueueManager.findByCodeQR(codeQR);
    }

    void changeQueueStatus(String codeQR, QueueStatusEnum queueStatus) {
        tokenQueueManager.changeQueueStatus(codeQR, queueStatus);
    }
}
