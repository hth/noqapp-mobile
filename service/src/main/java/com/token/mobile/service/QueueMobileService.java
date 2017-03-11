package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.BizStoreEntity;
import com.token.domain.QueueEntity;
import com.token.domain.TokenQueueEntity;
import com.token.domain.json.JsonQueue;
import com.token.domain.json.JsonToken;
import com.token.domain.json.JsonTokenAndQueue;
import com.token.domain.types.QueueStatusEnum;
import com.token.domain.types.QueueUserStateEnum;
import com.token.repository.QueueManager;
import com.token.repository.QueueManagerJDBC;
import com.token.service.BizService;

import java.util.ArrayList;
import java.util.List;

/**
 * User: hitender
 * Date: 1/9/17 12:30 PM
 */
@Service
public class QueueMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(QueueMobileService.class);

    private QueueManager queueManager;
    private TokenQueueMobileService tokenQueueMobileService;
    private BizService bizService;
    private QueueManagerJDBC queueManagerJDBC;

    @Autowired
    public QueueMobileService(
            QueueManager queueManager,
            TokenQueueMobileService tokenQueueMobileService,
            BizService bizService,
            QueueManagerJDBC queueManagerJDBC
    ) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizService = bizService;
        this.queueManagerJDBC = queueManagerJDBC;
    }

    /**
     * When merchant has served a specific token.
     *
     * @param codeQR
     * @param servedNumber
     * @param queueUserState
     * @return
     */
    public JsonToken updateAndGetNextInQueue(String codeQR, int servedNumber, QueueUserStateEnum queueUserState) {
        LOG.info("Getting queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, queueUserState);
        QueueEntity queue = queueManager.updateAndGetNextInQueue(codeQR, servedNumber, queueUserState);
        if (null != queue) {
            LOG.info("Found queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, queueUserState);
            return tokenQueueMobileService.updateServing(codeQR, QueueStatusEnum.N, queue.getTokenNumber());
        }

        LOG.info("Reached condition of not having any more to serve");
        TokenQueueEntity tokenQueue = tokenQueueMobileService.findByCodeQR(codeQR);
        tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.D);
        return new JsonToken(codeQR)
                .setToken(servedNumber)
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setDisplayName(tokenQueue.getDisplayName())
                .setQueueStatus(QueueStatusEnum.D);
    }

    /**
     * Merchant when starting or re-starting to serve token when QueueState has been either Start or Re-Start.
     *
     * @param codeQR
     * @return
     */
    public JsonToken getNextInQueue(String codeQR) {
        LOG.info("Getting queue codeQR={}", codeQR);
        QueueEntity queue = queueManager.getNext(codeQR);
        if (null != queue) {
            LOG.info("Found queue codeQR={}", codeQR);
            JsonToken jsonToken = tokenQueueMobileService.updateServing(codeQR, QueueStatusEnum.N, queue.getTokenNumber());
            tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.N);
            return jsonToken;
        }

        return null;
    }

    public List<JsonTokenAndQueue> findAllJoinedQueues(String did) {
        List<QueueEntity> queues = queueManager.findAllByDid(did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, null);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(jsonToken.getToken(), jsonToken.getQueueStatus(), jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        return jsonTokenAndQueues;
    }

    public List<JsonTokenAndQueue> findHistoricalQueue(String did) {
        List<QueueEntity> queues = queueManagerJDBC.findByDid(did);

        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            BizStoreEntity bizStore = bizService.findByCodeQR(queue.getCodeQR());
            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(queue, bizStore);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        return jsonTokenAndQueues;
    }
}
