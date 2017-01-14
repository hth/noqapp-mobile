package com.token.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.token.domain.QueueEntity;
import com.token.domain.json.JsonToken;
import com.token.domain.types.QueueStateEnum;
import com.token.repository.QueueManager;

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
            return tokenQueueMobileService.updateServing(codeQR, queue.getTokenNumber());
        }

        return null;
    }
}
