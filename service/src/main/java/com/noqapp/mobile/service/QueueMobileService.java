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
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.JsonQueuedPerson;
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
import com.noqapp.service.QueueService;
import com.noqapp.utils.Validate;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newCachedThreadPool;

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
    private QueueService queueService;

    private ExecutorService service;

    @Autowired
    public QueueMobileService(
            QueueManager queueManager,
            TokenQueueMobileService tokenQueueMobileService,
            BizService bizService,
            DeviceService deviceService,
            QueueManagerJDBC queueManagerJDBC,
            StoreHourManager storeHourManager,
            QueueService queueService) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizService = bizService;
        this.deviceService = deviceService;
        this.queueManagerJDBC = queueManagerJDBC;
        this.storeHourManager = storeHourManager;
        this.queueService = queueService;

        this.service = newCachedThreadPool();
    }

    /**
     * When merchant has served a specific token.
     *
     * @param codeQR
     * @param servedNumber
     * @param queueUserState
     * @param goTo           - counter name
     * @param sid            - server device id
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
     * @param goTo   counter name
     * @param sid    server device id
     * @return
     */
    public JsonToken getNextInQueue(String codeQR, String goTo, String sid) {
        LOG.info("Getting next in queue for codeQR={} goTo={} sid={}", codeQR, goTo, sid);

        QueueEntity queue = queueManager.getNext(codeQR, goTo, sid);
        if (null != queue) {
            LOG.info("Found queue codeQR={} token={}", codeQR, queue.getTokenNumber());

            JsonToken jsonToken = tokenQueueMobileService.updateServing(
                    codeQR,
                    QueueStatusEnum.N,
                    queue.getTokenNumber(),
                    goTo);
            //TODO(hth) call can be put in thread
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

    /**
     * Merchant when serving a specific token in queue. This is works for out of order request in queue.
     *
     * @param codeQR
     * @param goTo   counter name
     * @param sid    server device id
     * @param token  specific token being requested for next service
     * @return
     */
    public JsonToken getThisAsNextInQueue(String codeQR, String goTo, String sid, int token) {
        LOG.info("Getting specific token next in queue for codeQR={} goTo={} sid={} token={}",
                codeQR,
                goTo,
                sid,
                token);

        QueueEntity queue = queueManager.getThisAsNext(codeQR, goTo, sid, token);
        if (null != queue) {
            LOG.info("Found queue codeQR={} token={}", codeQR, queue.getTokenNumber());

            JsonToken jsonToken = tokenQueueMobileService.updateThisServing(
                    codeQR,
                    QueueStatusEnum.N,
                    queue.getTokenNumber(),
                    goTo);
            //TODO(hth) call can be put in thread
            tokenQueueMobileService.changeQueueStatus(codeQR, QueueStatusEnum.N);
            return jsonToken;
        }

        return null;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String did) {
        List<QueueEntity> queues = queueManager.findAllQueuedByDid(did);
        LOG.info("Currently joined queue size={} did={}", queues.size(), did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);

            /*
             * Join Queue will join if user is not joined, hence fetch only queues with status is Queued.
             * Since we are fetching only queues that are joined, we can send averageServiceTime as zero.
             */
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, null, 0);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(
                    jsonToken.getToken(),
                    jsonToken.getQueueStatus(),
                    jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Current queue={} did={}", jsonTokenAndQueueList, did);

        return jsonTokenAndQueueList;
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String qid, String did) {
        Validate.isValidQid(qid);
        List<QueueEntity> queues = queueService.findAllQueuedByQid(qid);
        LOG.info("Currently joined queue size={} rid={} did={}", queues.size(), qid, did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);
            
            /*
             * Join Queue will join if user is not joined, hence fetch only queues with status is Queued.
             * Since we are fetching only queues that are joined, we can send averageServiceTime as zero.
             */
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, qid, 0);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(
                    jsonToken.getToken(),
                    jsonToken.getQueueStatus(),
                    jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Current queue={} rid={} did={}", jsonTokenAndQueueList, qid, did);

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

    public JsonTokenAndQueueList findHistoricalQueue(String did, DeviceTypeEnum deviceType, String token) {
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(null, did, token);

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByDid(did);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByDid(did);
            deviceService.registerDevice(null, did, deviceType, token);
            LOG.info("Historical new device queue size={} did={} deviceType={}", historyQueues.size(), did, deviceType);
        } else {
            /* Unset QID for DID as user seems to have logged out of the App. */
            if (StringUtils.isNotBlank(registeredDevice.getQueueUserId())) {
                deviceService.unsetQidForDevice(registeredDevice.getId());
            }

            /*
             * When device is marked for getting data since beginning, or request came without
             * QID but device has QID then get historical data until one year old.
             */
            sinceBeginning = registeredDevice.isSinceBeginning() || StringUtils.isNotBlank(registeredDevice.getQueueUserId());
            Date fetchUntil = computeDateToFetchSince(deviceType, registeredDevice, sinceBeginning);
            historyQueues = queueManagerJDBC.getByDid(did, fetchUntil);

            markFetchedSinceBeginningForDevice(registeredDevice);
            LOG.info("Historical existing device queue size={} did={} deviceType={}",
                    historyQueues.size(),
                    did,
                    deviceType);
        }

        servicedQueues.addAll(historyQueues);

        LOG.info("Historical queue size={} did={} deviceType={}", servicedQueues.size(), did, deviceType);
        return getJsonTokenAndQueueList(servicedQueues, sinceBeginning);
    }

    public JsonTokenAndQueueList findHistoricalQueue(String qid, String did, DeviceTypeEnum deviceType, String token) {
        Validate.isValidQid(qid);
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(qid, did, token);

        /* Get all the queues that have been serviced for today. This first for sorting reasons. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByQid(qid);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByQid(qid);
            deviceService.registerDevice(qid, did, deviceType, token);
            LOG.info("Historical new device queue size={} did={} qid={} deviceType={}",
                    historyQueues.size(),
                    did,
                    qid,
                    deviceType);

        } else {
            if (StringUtils.isBlank(registeredDevice.getQueueUserId())) {
                /* Save with QID when missing in registered device. */
                deviceService.registerDevice(qid, did, deviceType, token);
            }

            /*
             * When device is marked for getting data since beginning,
             * then get historical data until one year old.
             */
            sinceBeginning = registeredDevice.isSinceBeginning();
            Date fetchUntil = computeDateToFetchSince(deviceType, registeredDevice, sinceBeginning);
            historyQueues = queueManagerJDBC.getByQid(qid, fetchUntil);

            markFetchedSinceBeginningForDevice(registeredDevice);
            LOG.info("Historical existing device queue size={} did={} qid={} deviceType={}",
                    historyQueues.size(),
                    did,
                    qid,
                    deviceType);
        }

        servicedQueues.addAll(historyQueues);

        LOG.info("Historical queue size={} qid={} did={} deviceType={}", servicedQueues.size(), qid, did, deviceType);
        return getJsonTokenAndQueueList(servicedQueues, sinceBeginning);
    }

    private Date computeDateToFetchSince(
            DeviceTypeEnum deviceType,
            RegisteredDeviceEntity registeredDevice,
            boolean sinceBeginning
    ) {
        Date fetchUntil;
        switch (deviceType) {
            case A:
                fetchUntil = sinceBeginning ? DateTime.now().minusYears(1).toDate() : registeredDevice.getUpdated();
                break;
            case I:
                /* Get until a year old data for iPhone since it does not have database. */
                fetchUntil = DateTime.now().minusYears(1).toDate();
                break;
            default:
                LOG.error("Reached unsupported deviceType {}", deviceType);
                throw new UnsupportedOperationException("Reached unsupported deviceType " + deviceType.getDescription());
        }

        return fetchUntil;
    }

    private void markFetchedSinceBeginningForDevice(RegisteredDeviceEntity registeredDevice) {
        if (registeredDevice.isSinceBeginning()) {
            deviceService.markFetchedSinceBeginningForDevice(registeredDevice.getId());
        }
    }

    private JsonTokenAndQueueList getJsonTokenAndQueueList(List<QueueEntity> queues, boolean sinceBeginning) {
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            try {
                //TODO(hth) might need add null check for bizStore
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

        return new JsonTokenAndQueueList()
                .setTokenAndQueues(jsonTokenAndQueues)
                .setSinceBeginning(sinceBeginning);
    }

    /**
     * Since review can be done in background. Moved logic to thread.
     *
     * @param codeQR
     * @param token
     * @param did
     * @param qid
     * @param ratingCount
     * @param hoursSaved
     */
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved) {
        service.submit(() -> reviewingService(codeQR, token, did, qid, ratingCount, hoursSaved));
        return true;
    }

    /**
     * Submitting review.
     */
    private void reviewingService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved) {
        boolean reviewSubmitStatus = queueManager.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved);
        if (!reviewSubmitStatus) {
            reviewSubmitStatus = reviewHistoricalService(codeQR, token, did, qid, ratingCount, hoursSaved);
        }

        LOG.info("Review update status={} codeQR={} token={} ratingCount={} hoursSaved={} did={} qid={} ",
                reviewSubmitStatus,
                codeQR,
                token,
                ratingCount,
                hoursSaved,
                did,
                qid);
    }

    private boolean reviewHistoricalService(
            String codeQR,
            int token,
            String did,
            String qid,
            int ratingCount,
            int hoursSaved
    ) {
        return queueManagerJDBC.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved);
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

    /**
     * Finds clients who are yet to be serviced.
     *
     * @param codeQR
     * @return
     */
    public JsonQueuePersonList findAllClientToBeServiced(String codeQR) {
        List<JsonQueuedPerson> queuedPeople = new ArrayList<>();

        List<QueueEntity> queues = queueManager.findAllClientToBeServiced(codeQR);
        for (QueueEntity queue : queues) {
            JsonQueuedPerson jsonQueuedPerson = new JsonQueuedPerson()
                    .setCustomerName(queue.getCustomerName())
                    .setToken(queue.getTokenNumber())
                    .setServerDeviceId(queue.getServerDeviceId());

            queuedPeople.add(jsonQueuedPerson);
        }

        return new JsonQueuePersonList().setQueuedPeople(queuedPeople);
    }
}
