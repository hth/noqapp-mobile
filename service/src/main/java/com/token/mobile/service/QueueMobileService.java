package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.QueueEntity;
import com.token.domain.TokenQueueEntity;
import com.token.domain.json.JsonQueue;
import com.token.domain.json.JsonToken;
import com.token.domain.json.JsonTokenAndQueue;
import com.token.domain.types.QueueStateEnum;
import com.token.domain.types.QueueStatusEnum;
import com.token.repository.QueueManager;

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

    @Autowired
    public QueueMobileService(QueueManager queueManager, TokenQueueMobileService tokenQueueMobileService) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
    }

    public JsonToken updateAndGetNextInQueue(String codeQR, int servedNumber, QueueStateEnum queueState) {
        LOG.info("Getting queue codeQR={} servedNumber={} queueState={}", codeQR, servedNumber, queueState);
        QueueEntity queue = queueManager.updateAndGetNextInQueue(codeQR, servedNumber, queueState);
        if (null != queue) {
            LOG.info("Found queue codeQR={} servedNumber={} queueState={}", codeQR, servedNumber, queueState);
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
}
