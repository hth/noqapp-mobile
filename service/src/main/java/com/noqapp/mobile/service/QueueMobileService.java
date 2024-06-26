package com.noqapp.mobile.service;

import static com.noqapp.common.utils.DateUtil.DAY.TODAY;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apiguardian.api.API.Status.DEPRECATED;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.Validate;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserEntity;
import com.noqapp.domain.PointEarnedEntity;
import com.noqapp.domain.PurchaseOrderEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonQueueHistoricalList;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTokenAndQueue;
import com.noqapp.domain.json.JsonTokenAndQueueList;
import com.noqapp.domain.types.AppFlavorEnum;
import com.noqapp.domain.types.DeviceTypeEnum;
import com.noqapp.domain.types.PointActivityEnum;
import com.noqapp.domain.types.SentimentTypeEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.mobile.domain.JsonStoreSetting;
import com.noqapp.mobile.service.exception.DeviceDetailMissingException;
import com.noqapp.repository.BusinessUserManager;
import com.noqapp.repository.PointEarnedManager;
import com.noqapp.repository.QueueManager;
import com.noqapp.repository.QueueManagerJDBC;
import com.noqapp.repository.ScheduleAppointmentManager;
import com.noqapp.repository.StoreHourManager;
import com.noqapp.repository.UserProfileManager;
import com.noqapp.service.BizService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.PurchaseOrderProductService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.QueueService;
import com.noqapp.service.StoreHourService;
import com.noqapp.service.nlp.NLPService;

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.apiguardian.api.API;

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
    private QueueManagerJDBC queueManagerJDBC;
    private StoreHourManager storeHourManager;
    private BusinessUserManager businessUserManager;
    private UserProfileManager userProfileManager;
    private ScheduleAppointmentManager scheduleAppointmentManager;
    private PointEarnedManager pointEarnedManager;
    private BizService bizService;
    private DeviceRegistrationService deviceRegistrationService;
    private NLPService nlpService;
    private PurchaseOrderService purchaseOrderService;
    private PurchaseOrderProductService purchaseOrderProductService;
    private QueueService queueService;
    private JoinAbortService joinAbortService;
    private TokenQueueMobileService tokenQueueMobileService;
    private JMSProducerMobileService jmsProducerMobileService;
    private StoreHourService storeHourService;

    private ExecutorService executorService;

    @Autowired
    public QueueMobileService(
        QueueManager queueManager,
        QueueManagerJDBC queueManagerJDBC,
        StoreHourManager storeHourManager,
        BusinessUserManager businessUserManager,
        UserProfileManager userProfileManager,
        ScheduleAppointmentManager scheduleAppointmentManager,
        PointEarnedManager pointEarnedManager,
        BizService bizService,
        DeviceRegistrationService deviceRegistrationService,
        NLPService nlpService,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderProductService purchaseOrderProductService,
        QueueService queueService,
        JoinAbortService joinAbortService,
        TokenQueueMobileService tokenQueueMobileService,
        JMSProducerMobileService jmsProducerMobileService,
        StoreHourService storeHourService
    ) {
        this.queueManager = queueManager;
        this.queueManagerJDBC = queueManagerJDBC;
        this.storeHourManager = storeHourManager;
        this.businessUserManager = businessUserManager;
        this.userProfileManager = userProfileManager;
        this.scheduleAppointmentManager = scheduleAppointmentManager;
        this.pointEarnedManager = pointEarnedManager;
        this.bizService = bizService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.nlpService = nlpService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderProductService = purchaseOrderProductService;
        this.queueService = queueService;
        this.joinAbortService = joinAbortService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.jmsProducerMobileService = jmsProducerMobileService;
        this.storeHourService = storeHourService;

        this.executorService = newCachedThreadPool();
    }

    @API(status = DEPRECATED, since = "1.3.121")
    /* Note: Since un-registered user, return blank jsonTokenAndQueueList. */
    public JsonTokenAndQueueList findAllJoinedQueues(String did) {
        if (StringUtils.isBlank(did)) {
            LOG.warn("DID is blank");
            throw new DeviceDetailMissingException("DID should not be blank");
        }

        List<QueueEntity> queues = queueManager.findAllQueuedByDid(did);
        LOG.info("Currently joined queue size={} did={}", queues.size(), did);
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            queueService.validateJoinedQueue(queue);

            /*
             * Join Queue will join if user is not joined, hence fetch only queues with status is Queued.
             * Since we are fetching only queues that are joined, we can send averageServiceTime as zero, and
             * tokenService as null
             */
            BizStoreEntity bizStore = bizService.findByCodeQR(queue.getCodeQR());
            JsonToken jsonToken = joinAbortService.joinQueue(did, null, null, bizStore, null);
            JsonQueue jsonQueue = queueService.findTokenState(queue.getCodeQR());

            /* Override the create date of TokenAndQueue. This date helps in sorting of client side to show active queue. */
            jsonQueue.setCreated(queue.getCreated());

            LOG.info("QID is {} should be null for did={}", queue.getQueueUserId(), did);
            JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(
                jsonToken.getToken(),
                jsonToken.getDisplayToken(),
                null,
                jsonToken.getQueueStatus(),
                jsonQueue,
                null);
            jsonTokenAndQueues.add(jsonTokenAndQueue);
        }

        JsonTokenAndQueueList jsonTokenAndQueueList = new JsonTokenAndQueueList();
        jsonTokenAndQueueList.setTokenAndQueues(jsonTokenAndQueues);
        LOG.info("Current tokenAndQueueSize={} did={}", jsonTokenAndQueueList.getTokenAndQueues().size(), did);

        return jsonTokenAndQueueList;
    }

    @API(status = DEPRECATED, since = "1.3.122")
    @Deprecated
    public JsonTokenAndQueueList findHistoricalQueue(
        String did,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String token,
        String model,
        String osVersion,
        String appVersion,
        String deviceLanguage,
        double[] coordinate,
        String ipAddress
    ) {
        RegisteredDeviceEntity registeredDevice = deviceRegistrationService.lastAccessed(null, did, token, model, osVersion, appVersion, deviceLanguage, ipAddress);

        /* Get all the queues that have been serviced for today. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByDid(did);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByDid(did);
            try {
                deviceRegistrationService.registerDevice(null, did, deviceType, appFlavor, token, model, osVersion, appVersion, deviceLanguage, coordinate, ipAddress);
            } catch (DeviceDetailMissingException e) {
                LOG.error("Failed registration as cannot find did={} token={} reason={}", did, token, e.getLocalizedMessage(), e);
                throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
            }
            LOG.info("Historical new device queue size={} did={} deviceType={}", historyQueues.size(), did, deviceType);
        } else {
            /* Unset QID for DID as user seems to have logged out of the App. */
            if (StringUtils.isNotBlank(registeredDevice.getQueueUserId())) {
                deviceRegistrationService.unsetQidForDevice(registeredDevice.getId());
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

    @API(status = DEPRECATED, since = "1.3.122")
    @Deprecated
    public JsonTokenAndQueueList findHistoricalQueue(
        String qid,
        String did,
        DeviceTypeEnum deviceType,
        AppFlavorEnum appFlavor,
        String token,
        String model,
        String osVersion,
        String appVersion,
        String deviceLanguage,
        double[] coordinate,
        String ipAddress
    ) {
        Validate.isValidQid(qid);
        RegisteredDeviceEntity registeredDevice = deviceRegistrationService.lastAccessed(qid, did, token, model, osVersion, appVersion, deviceLanguage, ipAddress);

        /* Get all the queues that have been serviced for today. This first for sorting reasons. */
        List<QueueEntity> servicedQueues = queueService.findAllNotQueuedByQid(qid);

        boolean sinceBeginning = false;
        List<QueueEntity> historyQueues;
        if (null == registeredDevice) {
            historyQueues = queueService.getByQid(qid);
            try {
                deviceRegistrationService.registerDevice(qid, did, deviceType, appFlavor, token, model, osVersion, appVersion, deviceLanguage, coordinate, ipAddress);
            } catch (DeviceDetailMissingException e) {
                LOG.error("Failed registration as cannot find did={} token={} reason={}", did, token, e.getLocalizedMessage(), e);
                throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
            }
            LOG.info("Historical new device queue size={} did={} qid={} deviceType={}", historyQueues.size(), did, qid, deviceType);
        } else {
            if (StringUtils.isBlank(registeredDevice.getQueueUserId())) {
                try {
                    /* Save with QID when missing in registered device. */
                    deviceRegistrationService.registerDevice(qid, did, deviceType, appFlavor, token, model, osVersion, appVersion, deviceLanguage, coordinate, ipAddress);
                } catch (DeviceDetailMissingException e) {
                    LOG.error("Failed registration as cannot find did={} token={} reason={}", did, token, e.getLocalizedMessage(), e);
                    throw new DeviceDetailMissingException("Something went wrong. Please restart the app.");
                }
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
            deviceRegistrationService.markFetchedSinceBeginningForDevice(registeredDevice.getId());
        }
    }

    @API(status = DEPRECATED, since = "1.3.122")
    @Deprecated
    private JsonTokenAndQueueList getJsonTokenAndQueueList(List<QueueEntity> queues, boolean sinceBeginning) {
        List<JsonTokenAndQueue> jsonTokenAndQueues = new ArrayList<>();
        for (QueueEntity queue : queues) {
            try {
                BizStoreEntity bizStore = bizService.findByCodeQR(queue.getCodeQR());

                /* Remove deleted store from history when displaying. */
                if (!bizStore.isDeleted()) {
                    /* Currently gets all hours for the week. Can be replaced with just the specific day. */
                    bizStore.setStoreHours(storeHourService.findAllStoreHours(bizStore.getId()));
                    LOG.debug("BizStore codeQR={} bizStoreId={}", queue.getCodeQR(), bizStore.getId());
                    JsonPurchaseOrder jsonPurchaseOrder = null;
                    if (StringUtils.isNotBlank(queue.getTransactionId())) {
                        PurchaseOrderEntity purchaseOrder = purchaseOrderService.findByTransactionId(queue.getTransactionId());
                        if (null == purchaseOrder) {
                            purchaseOrder = purchaseOrderService.findHistoricalPurchaseOrder(queue.getQueueUserId(), queue.getTransactionId());
                            jsonPurchaseOrder = purchaseOrderProductService.populateHistoricalJsonPurchaseOrder(purchaseOrder);
                        } else {
                            jsonPurchaseOrder = purchaseOrderProductService.populateJsonPurchaseOrder(purchaseOrder);
                        }
                    }
                    JsonTokenAndQueue jsonTokenAndQueue = new JsonTokenAndQueue(queue, bizStore, jsonPurchaseOrder);
                    jsonTokenAndQueues.add(jsonTokenAndQueue);
                } else {
                    LOG.warn("Store {} codeQR={} is deleted mark queue={} as in active", bizStore.getDisplayName(), bizStore.getCodeQR(), queue.getId());
                }
            } catch (Exception e) {
                LOG.error("Failed finding bizStore for codeQR={} reason={}", queue.getCodeQR(), e.getLocalizedMessage(), e);
            }
        }

        return new JsonTokenAndQueueList()
            .setTokenAndQueues(jsonTokenAndQueues);
    }

    /** Since review can be done in background. Moved logic to thread. */
    public boolean reviewService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved, String review) {
        executorService.submit(() -> reviewingService(codeQR, token, did, qid, ratingCount, hoursSaved, review));
        return true;
    }

    /** Submitting review. */
    private void reviewingService(String codeQR, int token, String did, String qid, int ratingCount, int hoursSaved, String review) {
        SentimentTypeEnum sentimentType = nlpService.computeSentiment(review);
        boolean reviewSubmitStatus = queueManager.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved, review, sentimentType);
        if (!reviewSubmitStatus) {
            //TODO(hth) make sure for Guardian this is taken care. Right now its ignore "GQ" add to MySQL Table
            reviewSubmitStatus = reviewHistoricalService(codeQR, token, did, qid, ratingCount, hoursSaved, review, sentimentType);
        }

        /* Add points on review submission. */
        if (reviewSubmitStatus && StringUtils.isNotBlank(review)) {
            pointEarnedManager.save(new PointEarnedEntity(qid, PointActivityEnum.REV));
        }

        sendMailWhenSentimentIsNegative(codeQR, token, ratingCount, hoursSaved, review, sentimentType);

        LOG.info("Review update status={} codeQR={} token={} ratingCount={} hoursSaved={} did={} qid={} review={} sentimentType={}",
            reviewSubmitStatus,
            codeQR,
            token,
            ratingCount,
            hoursSaved,
            did,
            qid,
            review,
            sentimentType);
    }

    private void sendMailWhenSentimentIsNegative(String codeQR, int token, int ratingCount, int hoursSaved, String review, SentimentTypeEnum sentimentType) {
        if (SentimentTypeEnum.N == sentimentType) {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
            List<BusinessUserEntity> businessUsers = businessUserManager.getAllForBusiness(bizStore.getBizName().getId(), UserLevelEnum.M_ADMIN);

            QueueEntity queue = queueManager.findOne(codeQR, token);
            for (BusinessUserEntity businessUser : businessUsers) {
                jmsProducerMobileService.invokeMailOnReviewNegative(
                    queue.getDisplayName(),
                    queue.getCustomerName(),
                    queue.getCustomerPhone(),
                    ratingCount,
                    hoursSaved,
                    review,
                    sentimentType.getDescription(),
                    userProfileManager.findByQueueUserId(businessUser.getQueueUserId()).getEmail());
            }
        }
    }

    private boolean reviewHistoricalService(
        String codeQR,
        int token,
        String did,
        String qid,
        int ratingCount,
        int hoursSaved,
        String review,
        SentimentTypeEnum sentimentType
    ) {
        //TODO(hth) when adding new review increase ratingCount. Make sure when editing review, do not increase count.
        return queueManagerJDBC.reviewService(codeQR, token, did, qid, ratingCount, hoursSaved, review, sentimentType);
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

    public StoreHourEntity updateQueueStateForToday(JsonStoreSetting modifyQueue) {
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
            modifyQueue.getLunchTimeStart(),
            modifyQueue.getLunchTimeEnd(),
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
        JsonQueuePersonList jsonQueuePersonList = queueService.findAllClient(codeQR);
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        String day = DateUtil.dateToString(DateUtil.dateAtTimeZone(bizStore.getTimeZone()));
        long appointmentCountForToday = scheduleAppointmentManager.countNumberOfAppointments(codeQR, day);
        LOG.info("Looking up appointments for {} {} {}", codeQR, day, appointmentCountForToday);
        jsonQueuePersonList.setAppointmentCountForToday(appointmentCountForToday);
        return jsonQueuePersonList;
    }

    public JsonQueuePersonList findAllRegisteredClientHistorical(String codeQR, Date start, Date until) {
        return queueService.findAllRegisteredClientHistorical(codeQR, start, until);
    }

    public BizStoreEntity findByCodeQR(String codeQR) {
        return bizService.findByCodeQR(codeQR);
    }

    public JsonQueueHistoricalList findAllHistoricalQueueAsJson(String qid) {
        return queueService.findAllHistoricalQueueAsJson(qid);
    }

    @Async
    public void updateUnregisteredUserWithNameAndPhone(String codeQR, int tokenNumber, String name, String phone) {
        queueManager.updateUnregisteredUserWithNameAndPhone(codeQR, tokenNumber, name, phone);
    }
}
