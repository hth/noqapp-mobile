package com.noqapp.mobile.service;

import static com.noqapp.common.utils.DateUtil.Day.TODAY;
import static java.util.concurrent.Executors.newCachedThreadPool;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.Validate;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTokenAndQueue;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.StoreHourManager;
import com.noqapp.service.BizService;
import com.noqapp.service.ExternalService;
import com.noqapp.service.QueueService;

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

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

    private ExecutorService executorService;

    @Autowired
    public QueueMobileService(
        QueueManager queueManager,
        TokenQueueMobileService tokenQueueMobileService,
        BizService bizService,
        DeviceService deviceService,
        QueueManagerJDBC queueManagerJDBC,
        StoreHourManager storeHourManager,
        QueueService queueService
    ) {
        this.queueManager = queueManager;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizService = bizService;
        this.deviceService = deviceService;
        this.queueManagerJDBC = queueManagerJDBC;
        this.storeHourManager = storeHourManager;
        this.queueService = queueService;

        this.executorService = newCachedThreadPool();
    }

    public JsonTokenAndQueueList findAllJoinedQueues(String did) {
        if (StringUtils.isBlank(did)) {
            throw new RuntimeException("DID should not be blank");
        }

        List<QueueEntity> queues = queueManager.findAllQueuedByDid(did);
        LOG.info("Currently joined queue size={} did={}", queues.size(), did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);

            /*
             * Join Queue will join if user is not joined, hence fetch only queues with status is Queued.
             * Since we are fetching only queues that are joined, we can send averageServiceTime as zero, and
             * tokenService as null
             */
            JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, null, null, 0, null);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            LOG.info("QID is {} should be null for did={}", queue.getQueueUserId(), did);
            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(
                jsonToken.getToken(),
                null,
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
        LOG.info("Currently joined queue size={} qid={} did={}", queues.size(), qid, did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            validateJoinedQueue(queue);

            /*
             * Join Queue will join if user is not joined, hence fetch only queues with status is Queued.
             * Since we are fetching only queues that are joined, we can send
             * averageServiceTime as zero, and
             * tokenService as null, and
             * guardianQid as null too.
             */
            //JsonToken jsonToken = tokenQueueMobileService.joinQueue(queue.getCodeQR(), did, qid, queue.getGuardianQid(), 0, null);
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(queue.getCodeQR());

            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(
                queue.getTokenNumber(),
                queue.getQueueUserId(),
                tokenQueueMobileService.findByCodeQR(queue.getCodeQR()).getQueueStatus(),
                jsonQueue);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Current queue={} qid={} did={}", jsonTokenAndQueueList, qid, did);

        return jsonTokenAndQueueList;
    }

    private void validateJoinedQueue(QueueEntity queue) {
        switch (queue.getQueueUserState()) {
            case A:
            case S:
            case N:
                LOG.error("Failed as only Q status is supported");
                throw new UnsupportedOperationException("Reached not supported condition");
        }
    }

    public JsonTokenAndQueueList findHistoricalQueue(String did, DeviceTypeEnum deviceType, AppFlavorEnum appFlavor, String token, String model, String osVersion) {
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(null, did, token, model, osVersion);

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByDid(did);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByDid(did);
            deviceService.registerDevice(null, did, deviceType, appFlavor, token, model, osVersion);
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

    public JsonTokenAndQueueList findHistoricalQueue(
        String qid,
        String did,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String token,
        String model,
        String osVersion
    ) {
        Validate.isValidQid(qid);
        RegisteredDeviceEntity registeredDevice = deviceService.lastAccessed(qid, did, token, model, osVersion);

        /* Get all the queues that have been serviced for today. This first for sorting reasons. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByQid(qid);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByQid(qid);
            deviceService.registerDevice(qid, did, deviceType, appFlavor, token, model, osVersion);
            LOG.info("Historical new device queue size={} did={} qid={} deviceType={}",
                historyQueues.size(),
                did,
                qid,
                deviceType);

        } else {
            if (StringUtils.isBlank(registeredDevice.getQueueUserId())) {
                /* Save with QID when missing in registered device. */
                deviceService.registerDevice(qid, did, deviceType, appFlavor, token, model, osVersion);
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
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved, String review) {
        executorService.submit(() -> reviewingService(codeQR, token, did, qid, ratingCount, hoursSaved, review));
        return true;
    }

    /**
     * Submitting review.
     */
    private void reviewingService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved, String review) {
        boolean reviewSubmitStatus = queueManager.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved, review);
        if (!reviewSubmitStatus) {
            //TODO(hth) make sure for Guardian this is taken care. Right now its ignore "GQ" add to MySQL Table
            reviewSubmitStatus = reviewHistoricalService(codeQR, token, did, qid, ratingCount, hoursSaved, review);
        }

        LOG.info("Review update status={} codeQR={} token={} ratingCount={} hoursSaved={} did={} qid={} review={}",
            reviewSubmitStatus,
            codeQR,
            token,
            ratingCount,
            hoursSaved,
            did,
            qid,
            review);
    }

    private boolean reviewHistoricalService(
        String codeQR,
        int token,
        String did,
        String qid,
        int ratingCount,
        int hoursSaved,
        String review
    ) {
        return queueManagerJDBC.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved, review);
    }

    public TokenQueueEntity getTokenQueueByCodeQR(String codeQR) {
        return tokenQueueMobileService.findByCodeQR(codeQR);
    }

    public StoreHourEntity getQueueStateForToday(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        return storeHourManager.findOne(bizStore.getId(), dayOfWeek);
    }

    public StoreHourEntity getQueueStateForTomorrow(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        DayOfWeek dayOfWeek = ZonedDateTime.now(TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId()).getDayOfWeek();
        return storeHourManager.findOne(bizStore.getId(), CommonUtil.getNextDayOfWeek(dayOfWeek));
    }

    public void resetTemporarySettingsOnStoreHour(String id) {
        Assert.hasText(id, "Should not be empty");
        storeHourManager.resetTemporarySettingsOnStoreHour(id);
    }

    public StoreHourEntity updateQueueStateForToday(JsonModifyQueue modifyQueue) {
        BizStoreEntity bizStore = bizService.findByCodeQR(modifyQueue.getCodeQR());
        TimeZone timeZone = TimeZone.getTimeZone(bizStore.getTimeZone());
        DayOfWeek dayOfWeek = ZonedDateTime.now(timeZone.toZoneId()).getDayOfWeek();
        StoreHourEntity today = storeHourManager.modifyOne(
            bizStore.getId(),
            dayOfWeek,
            modifyQueue.getTokenAvailableFrom(),
            modifyQueue.getStartHour(),
            modifyQueue.getTokenNotAvailableFrom(),
            modifyQueue.getEndHour(),
            modifyQueue.isDayClosed(),
            modifyQueue.isTempDayClosed(),
            modifyQueue.isPreventJoining(),
            modifyQueue.getDelayedInMinutes());
        updateNextRun(bizStore, today);
        return today;
    }

    public void updateNextRun(BizStoreEntity bizStore, StoreHourEntity today) {
        TimeZone timeZone = TimeZone.getTimeZone(bizStore.getTimeZone());
        /* Since store hour is being changed for today. We need to update the next run time for today. */
        int hourOfDay = today.storeClosingHourOfDay();
        int minuteOfDay = today.storeClosingMinuteOfDay();
        ZonedDateTime queueHistoryNextRun = DateUtil.computeNextRunTimeAtUTC(timeZone, hourOfDay, minuteOfDay, TODAY);
        bizService.updateNextRun(bizStore, Date.from(queueHistoryNextRun.toInstant()));
    }

    public JsonQueuePersonList findAllClient(String codeQR) {
        return queueService.findAllClient(codeQR);
    }

    public JsonQueuePersonList findAllClientHistorical(String codeQR) {
        return queueService.findAllClientHistorical(codeQR);
    }

    public BizStoreEntity findByCodeQR(String codeQR) {
        return bizService.findByCodeQR(codeQR);
    }

    @Async
    public void updateBizStoreAvailableTokenCount(int availableTokenCount, String codeQR) {
        LOG.debug("Changed count {} for codeQR={}", availableTokenCount, codeQR);
        bizService.updateBizStoreAvailableTokenCount(availableTokenCount, codeQR);
    }
}
