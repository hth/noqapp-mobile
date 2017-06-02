package com.noqapp.mobile.service;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTokenAndQueue;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.service.BizService;
import com.noqapp.utils.Validate;

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
    private DeviceService deviceService;
    private QueueManagerJDBC queueManagerJDBC;

    @Autowired
    public QueueMobileService(
            QueueManager queueManager,
            TokenQueueMobileService tokenQueueMobileService,
            BizService bizService,
            DeviceService deviceService,
            QueueManagerJDBC queueManagerJDBC
    ) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizService = bizService;
        this.deviceService = deviceService;
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
    public JsonToken updateAndGetNextInQueue(String codeQR, int servedNumber, QueueUserStateEnum queueUserState, String goTo) {
        LOG.info("Update and getting next in queue codeQR={} servedNumber={} queueUserState={} goTo={}",
                codeQR, servedNumber, queueUserState, goTo);

        QueueEntity queue = queueManager.updateAndGetNextInQueue(codeQR, servedNumber, queueUserState);
        if (null != queue) {
            LOG.info("Found queue codeQR={} servedNumber={} queueUserState={} nextToken={}",
                    codeQR, servedNumber, queueUserState, queue.getTokenNumber());
            
            return tokenQueueMobileService.updateServing(codeQR, QueueStatusEnum.N, queue.getTokenNumber(), goTo);
        }

        LOG.info("Reached condition of not having any more to serve");
        TokenQueueEntity tokenQueue = getTokenQueueByCodeQR(codeQR);
        tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.D);
        return new JsonToken(codeQR)
                /* Better to show last number than served number. This is to maintain consistent state. */
                .setToken(tokenQueue.getCurrentlyServing())
                .setServingNumber(tokenQueue.getCurrentlyServing())
                .setDisplayName(tokenQueue.getDisplayName())
                .setQueueStatus(QueueStatusEnum.D);
    }

    /**
     * Merchant when pausing to serve queue.
     * 
     * @param codeQR
     * @param servedNumber
     * @param queueUserState
     * @return
     */
    public JsonToken pauseServingQueue(String codeQR, int servedNumber, QueueUserStateEnum queueUserState) {
        LOG.info("Server person is now pausing for queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, queueUserState);

        boolean status = queueManager.updateServedInQueue(codeQR, servedNumber, queueUserState);
        LOG.info("Paused status={}", status);
        TokenQueueEntity tokenQueue = getTokenQueueByCodeQR(codeQR);
        return new JsonToken(codeQR)
                .setToken(servedNumber)
                .setServingNumber(servedNumber)
                .setDisplayName(tokenQueue.getDisplayName())
                .setQueueStatus(QueueStatusEnum.R);
    }

    /**
     * Merchant when starting or re-starting to serve token when QueueState has been either Start or Re-Start.
     *
     * @param codeQR
     * @return
     */
    public JsonToken getNextInQueue(String codeQR, String goTo) {
        LOG.info("Getting next in queue for codeQR={} goTo={}", codeQR, goTo);

        QueueEntity queue = queueManager.getNext(codeQR);
        if (null != queue) {
            LOG.info("Found queue codeQR={} token={}", codeQR, queue.getTokenNumber());
            
            JsonToken jsonToken = tokenQueueMobileService.updateServing(codeQR, QueueStatusEnum.N, queue.getTokenNumber(), goTo);
            tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.N);
            return jsonToken;
        }

        return null;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String did) {
        List<QueueEntity> queues = queueManager.findAllQueuedByDid(did);
        LOG.info("Currently joined queue size={}", queues.size());
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);

            /* Join Queue will join if user is not joined, hence fetch only queues with status is Queued. */
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, null);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(jsonToken.getToken(), jsonToken.getQueueStatus(), jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Current queue={}", jsonTokenAndQueueList);

        return jsonTokenAndQueueList;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String rid, String did) {
        Validate.isValidRid(rid);
        List<QueueEntity> queues = queueManager.findAllQueuedByRid(rid);
        LOG.info("Historical joined queue size={}", queues.size());
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);
            
            /* Join Queue will join if user is not joined, hence fetch only queues with status is Queued. */
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, rid);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(jsonToken.getToken(), jsonToken.getQueueStatus(), jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Historical queue={}", jsonTokenAndQueueList);

        return jsonTokenAndQueueList;
    }

    private void validateJoinedQueue(QueueEntity queue) {
        switch (queue.getQueueUserState()) {
            case A:
            case S:
            case N:
                LOG.error("Failed as only Q status is supported");
                throw new RuntimeException("Reached not supported condition");
        }
    }

    private List<QueueEntity> findAllNotQueuedByDid(String did) {
        return queueManager.findAllNotQueuedByDid(did);
    }

    private List<QueueEntity> findAllNotQueuedByRid(String rid) {
        return queueManager.findAllNotQueuedByRid(rid);
    }

    public JsonTokenAndQueueList findHistoricalQueue(String did, DeviceTypeEnum deviceType, String token) {
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(null, did, token);
        List<QueueEntity> queues;
        if (registeredDevice == null) {
            queues = queueManagerJDBC.getByDid(did);
            deviceService.registerDevice(null, did, deviceType, token);
        } else {
            queues = queueManagerJDBC.getByDid(did, registeredDevice.getUpdated());
        }

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = findAllNotQueuedByDid(did);
        if (queues != null) {
            queues.addAll(servicedQueues);
        } else {
            queues = servicedQueues;
        }

        return getJsonTokenAndQueueList(queues);
    }

    public JsonTokenAndQueueList findHistoricalQueue(String rid, String did, DeviceTypeEnum deviceType, String token) {
        Validate.isValidRid(rid);
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(rid, did, token);
        List<QueueEntity> queues;
        if (registeredDevice == null) {
            queues = queueManagerJDBC.getByRid(rid);
            deviceService.registerDevice(rid, did, deviceType, token);
        } else {
            if (StringUtils.isBlank(registeredDevice.getReceiptUserId())) {
                /* Save with RID when missing in registered device. */
                deviceService.registerDevice(rid, did, deviceType, token);
            }
            queues = queueManagerJDBC.getByRid(rid, registeredDevice.getUpdated());
        }

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = findAllNotQueuedByRid(rid);
        if (queues != null) {
            queues.addAll(servicedQueues);
        } else {
            queues = servicedQueues;
        }

        return getJsonTokenAndQueueList(queues);
    }

    private JsonTokenAndQueueList getJsonTokenAndQueueList(List<QueueEntity> queues) {
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            try {
                BizStoreEntity bizStore = bizService.findByCodeQR(queue.getCodeQR());
                LOG.debug("BizStore codeQR={} bizStoreId={}", queue.getCodeQR(), bizStore.getId());
                JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(queue, bizStore);
                jsonTokenAndQueues.add(jsonTokenAndQueue);
            } catch (Exception e) {
                LOG.error("Failed finding bizStore for codeQR={} reason={}", queue.getCodeQR(), e.getLocalizedMessage(), e);
            }
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);

        return jsonTokenAndQueueList;
    }

    public boolean reviewService(String codeQR, int token, String did, String rid, int ratingCount, int hoursSaved) {
        boolean success = queueManager.reviewService(codeQR, token, did, rid, ratingCount, hoursSaved);
        if (!success) {
            success = reviewHistoricalService(codeQR, token, did, rid, ratingCount, hoursSaved);
        }
        return success;
    }

    private boolean reviewHistoricalService(String codeQR, int token, String did, String rid, int ratingCount, int hoursSaved) {
        return queueManagerJDBC.reviewService(codeQR, token, did, rid, ratingCount, hoursSaved);
    }

    public TokenQueueEntity getTokenQueueByCodeQR(String codeQR) {
        return tokenQueueMobileService.findByCodeQR(codeQR);
    }
}
