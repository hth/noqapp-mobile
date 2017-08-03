package com.noqapp.mobile.service;

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.StoreHourEntity;
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
import com.noqapp.repository.StoreHourManager;
import com.noqapp.service.BizService;
import com.noqapp.utils.Validate;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
    private StoreHourManager storeHourManager;

    @Autowired
    public QueueMobileService(
            QueueManager queueManager,
            TokenQueueMobileService tokenQueueMobileService,
            BizService bizService,
            DeviceService deviceService,
            QueueManagerJDBC queueManagerJDBC,
            StoreHourManager storeHourManager
    ) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizService = bizService;
        this.deviceService = deviceService;
        this.queueManagerJDBC = queueManagerJDBC;
        this.storeHourManager = storeHourManager;
    }

    /**
     * When merchant has served a specific token.
     *
     * @param codeQR
     * @param servedNumber
     * @param queueUserState
     * @param goTo - counter name
     * @param sid - server device id
     * @return
     */
    public JsonToken updateAndGetNextInQueue(
            String codeQR,
            int servedNumber,
            QueueUserStateEnum queueUserState,
            String goTo,
            String sid
    ) {
        LOG.info("Update and getting next in queue codeQR={} servedNumber={} queueUserState={} goTo={} sid={}",
                codeQR, servedNumber, queueUserState, goTo, sid);

        QueueEntity queue = queueManager.updateAndGetNextInQueue(codeQR, servedNumber, queueUserState, goTo, sid);
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
     * @param sid
     * @return
     */
    public JsonToken pauseServingQueue(String codeQR, int servedNumber, QueueUserStateEnum queueUserState, String sid) {
        LOG.info("Server person is now pausing for queue codeQR={} servedNumber={} queueUserState={} sid={}",
                codeQR, servedNumber, queueUserState, sid);

        boolean status = queueManager.updateServedInQueue(codeQR, servedNumber, queueUserState, sid);
        LOG.info("Paused status={}", status);
        TokenQueueEntity tokenQueue = getTokenQueueByCodeQR(codeQR);
        return new JsonToken(codeQR)
                .setToken(tokenQueue.getLastNumber())
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
    public JsonToken getNextInQueue(String codeQR, String goTo, String sid) {
        LOG.info("Getting next in queue for codeQR={} goTo={} sid={}", codeQR, goTo, sid);

        QueueEntity queue = queueManager.getNext(codeQR, goTo, sid);
        if (null != queue) {
            LOG.info("Found queue codeQR={} token={}", codeQR, queue.getTokenNumber());
            
            JsonToken jsonToken = tokenQueueMobileService.updateServing(codeQR, QueueStatusEnum.N, queue.getTokenNumber(), goTo);
            tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.N);
            return jsonToken;
        }

        /* When nothing is found, return DONE status for the queue. */
        TokenQueueEntity tokenQueue = getTokenQueueByCodeQR(codeQR);
        if (null != tokenQueue) {
            LOG.info("On next, found no one in queue, returning with DONE status");
            return new JsonToken(codeQR)
                    .setToken(tokenQueue.getLastNumber())
                    .setServingNumber(tokenQueue.getLastNumber())
                    .setDisplayName(tokenQueue.getDisplayName())
                    .setQueueStatus(QueueStatusEnum.D);
        }

        return null;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String did) {
        List<QueueEntity> queues = queueManager.findAllQueuedByDid(did);
        LOG.info("Currently joined queue size={} did={}", queues.size(), did);
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
        LOG.info("Current queue={} did={}", jsonTokenAndQueueList, did);

        return jsonTokenAndQueueList;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String rid, String did) {
        Validate.isValidRid(rid);
        List<QueueEntity> queues = queueManager.findAllQueuedByRid(rid);
        LOG.info("Currently joined queue size={} rid={} did={}", queues.size(), rid, did);
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
        LOG.info("Current queue={} rid={} did={}", jsonTokenAndQueueList, rid, did);

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
        if (null == registeredDevice) {
            queues = queueManagerJDBC.getByDid(did);
            deviceService.registerDevice(null, did, deviceType, token);
            LOG.info("Historical new device queue size={} did={} deviceType={}", queues.size(), did, deviceType);
        } else {
            /* When new device registration, then get data until one year old. */
            Date fetchUntil = registeredDevice.isSinceBeginning() ? DateTime.now().minusYears(1).toDate() : registeredDevice.getUpdated();
            queues = queueManagerJDBC.getByDid(did, fetchUntil);

            if (registeredDevice.isSinceBeginning()) {
                deviceService.markFetchedSinceBeginningForDevice(registeredDevice.getId());
            }
            LOG.info("Historical existing device queue size={} did={} deviceType={}", queues.size(), did, deviceType);
        }

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = findAllNotQueuedByDid(did);
        queues.addAll(servicedQueues);

        LOG.info("Historical queue size={} did={} deviceType={}", queues.size(), did, deviceType);
        return getJsonTokenAndQueueList(queues);
    }

    public JsonTokenAndQueueList findHistoricalQueue(String rid, String did, DeviceTypeEnum deviceType, String token) {
        Validate.isValidRid(rid);
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(rid, did, token);
        List<QueueEntity> queues;
        if (null == registeredDevice) {
            queues = queueManagerJDBC.getByRid(rid);
            deviceService.registerDevice(rid, did, deviceType, token);
            LOG.info("Historical new device queue size={} did={} rid={} deviceType={}", queues.size(), did, rid, deviceType);
        } else {
            if (StringUtils.isBlank(registeredDevice.getReceiptUserId())) {
                /* Save with RID when missing in registered device. */
                deviceService.registerDevice(rid, did, deviceType, token);
            }
            /* When new device registration, then get data until one year old. */
            Date fetchUntil = registeredDevice.isSinceBeginning() ? DateTime.now().minusYears(1).toDate() : registeredDevice.getUpdated();
            queues = queueManagerJDBC.getByRid(rid, fetchUntil);

            if (registeredDevice.isSinceBeginning()) {
                deviceService.markFetchedSinceBeginningForDevice(registeredDevice.getId());
            }
            LOG.info("Historical existing device queue size={} did={} rid={} deviceType={}", queues.size(), did, rid, deviceType);
        }

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = findAllNotQueuedByRid(rid);
        queues.addAll(servicedQueues);

        LOG.info("Historical queue size={} rid={} did={} deviceType={}", queues.size(), rid, did, deviceType);
        return getJsonTokenAndQueueList(queues);
    }

    private JsonTokenAndQueueList getJsonTokenAndQueueList(List<QueueEntity> queues) {
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            try {
                BizStoreEntity bizStore = bizService.findByCodeQR(queue.getCodeQR());
                /* Currently gets all hours for the week. Can be replaced with just the specific day. */
                bizStore.setStoreHours(bizService.findAllStoreHours(bizStore.getId()));
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
        boolean reviewSubmitStatus = queueManager.reviewService(codeQR, token, did, rid, ratingCount, hoursSaved);
        if (!reviewSubmitStatus) {
            reviewSubmitStatus = reviewHistoricalService(codeQR, token, did, rid, ratingCount, hoursSaved);
        }
        return reviewSubmitStatus;
    }

    private boolean reviewHistoricalService(String codeQR, int token, String did, String rid, int ratingCount, int hoursSaved) {
        return queueManagerJDBC.reviewService(codeQR, token, did, rid, ratingCount, hoursSaved);
    }

    public TokenQueueEntity getTokenQueueByCodeQR(String codeQR) {
        return tokenQueueMobileService.findByCodeQR(codeQR);
    }

    public StoreHourEntity getQueueStateForToday(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        return storeHourManager.findOne(bizStore.getId(), dayOfWeek);
    }

    public StoreHourEntity updateQueueStateForToday(String codeQR, boolean dayClosed, boolean preventJoining) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        return storeHourManager.modifyOne(bizStore.getId(), dayOfWeek, preventJoining, dayClosed);
    }
}
