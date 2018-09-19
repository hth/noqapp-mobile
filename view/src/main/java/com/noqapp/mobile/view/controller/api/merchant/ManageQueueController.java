package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.utils.DateUtil.Day.TODAY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MERCHANT_COULD_NOT_ACQUIRE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_ACTION_NOT_PERMITTED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.ScheduledTaskEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonBusinessCustomerLookup;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.ScheduleTaskEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.mobile.domain.body.merchant.ChangeUserInQueue;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.repository.ScheduledTaskManager;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.QueueService;
import com.noqapp.service.TokenQueueService;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

/**
 * Managed by merchant
 * User: hitender
 * Date: 1/9/17 10:15 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/m/mq")
public class ManageQueueController {
    private static final Logger LOG = LoggerFactory.getLogger(ManageQueueController.class);

    private int counterNameLength;
    private AuthenticateMobileService authenticateMobileService;
    private QueueService queueService;
    private QueueMobileService queueMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private TokenQueueService tokenQueueService;
    private TokenQueueMobileService tokenQueueMobileService;
    private AccountService accountService;
    private BusinessCustomerService businessCustomerService;
    private BizService bizService;
    private ScheduledTaskManager scheduledTaskManager;
    private ApiHealthService apiHealthService;

    @Autowired
    public ManageQueueController(
            @Value("${ManageQueueController.counterNameLength}")
            int counterNameLength,

            AuthenticateMobileService authenticateMobileService,
            QueueService queueService,
            QueueMobileService queueMobileService,
            BusinessUserStoreService businessUserStoreService,
            TokenQueueService tokenQueueService,
            TokenQueueMobileService tokenQueueMobileService,
            AccountService accountService,
            BusinessCustomerService businessCustomerService,
            BizService bizService,
            ScheduledTaskManager scheduledTaskManager,
            ApiHealthService apiHealthService
    ) {
        this.counterNameLength = counterNameLength;
        this.authenticateMobileService = authenticateMobileService;
        this.queueService = queueService;
        this.queueMobileService = queueMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.tokenQueueService = tokenQueueService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.accountService = accountService;
        this.businessCustomerService = businessCustomerService;
        this.bizService = bizService;
        this.scheduledTaskManager = scheduledTaskManager;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
            value = "/queues",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/queues by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonTopicList topics = new JsonTopicList();
            topics.setTopics(businessUserStoreService.getQueues(qid));
            return topics.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queues",
                    "getQueues",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * When client is served by merchant.
     * And
     * When client starts to serve for first time or re-start after serving the last in the queue.
     */
    @PostMapping(
            value = "/served",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String served(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Served mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/served by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("qr") ? map.get("qr").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR)) {
                LOG.info("Un-authorized store access to /api/m/mq/served by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveTokenString = map.containsKey("t") ? map.get("t").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveTokenString)) {
                servedNumber = Integer.parseInt(serveTokenString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} qid={}", serveTokenString, codeQR, qid);
                return getErrorReason("Not a valid number.", MOBILE_JSON);
            }

            QueueUserStateEnum queueUserState;
            try {
                queueUserState = map.containsKey("q") ? QueueUserStateEnum.valueOf(map.get("q").getText()) : null;
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueUserState reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue user state.", MOBILE_JSON);
            }

            QueueStatusEnum queueStatus;
            try {
                queueStatus = map.containsKey("s") ? QueueStatusEnum.valueOf(map.get("s").getText()) : null;
                Assert.notNull(queueStatus, "Queue Status cannot be null");
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueStatus reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            String goTo = map.containsKey("g") ? map.get("g").getText() : null;
            if (StringUtils.isBlank(goTo)) {
                return getErrorReason("Counter name cannot be empty.", MOBILE_JSON);
            } else {
                if (goTo.length() > counterNameLength) {
                    return getErrorReason("Counter name cannot exceed character size of 20.", MOBILE_JSON);
                }
            }

            TokenQueueEntity tokenQueue = tokenQueueService.findByCodeQR(codeQR);
            LOG.info("queueStatus received={} found={}", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case C:
                case D:
                case N:
                    if (queueStatus == QueueStatusEnum.P) {
                        if (queueUserState == QueueUserStateEnum.S) {
                            jsonToken = queueService.pauseServingQueue(codeQR, servedNumber, queueUserState, did.getText(), TokenServiceEnum.M);
                        } else {
                            return getErrorReason("Cannot pause until the last person has been served", MOBILE);
                        }
                    } else {
                        jsonToken = queueService.updateAndGetNextInQueue(codeQR, servedNumber, queueUserState, goTo, did.getText(), TokenServiceEnum.M);
                    }
                    break;
                case R:
                case S:
                    jsonToken = queueService.getNextInQueue(codeQR, goTo, did.getText());

                    /* Remove delay or any setting associated before starting of queue. */
                    LOG.info("Resetting queue when queue status={}", tokenQueue.getQueueStatus());
                    tokenQueueService.resetQueueSettingWhenQueueStarts(codeQR);
                    break;
                default:
                    LOG.error("Reached unsupported condition queueState={}", tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueState " + tokenQueue.getQueueStatus().getDescription());
            }

            if (null == jsonToken) {
                LOG.error("Could not find queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, queueUserState);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            LOG.info("On served response servedNumber={} nowServicing={} jsonToken={}", servedNumber, jsonToken.getServingNumber(), jsonToken);
            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/served",
                    "served",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Most called during refresh or reload of the app.
     */
    @GetMapping (
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Single queue associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/queue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/queue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            TokenQueueEntity tokenQueue = tokenQueueService.findByCodeQR(codeQR.getText());
            if (null == tokenQueue) {
                LOG.error("Failed finding codeQR={} by mail={}", codeQR.getText(), mail);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            return new JsonTopic(tokenQueue).setHour(businessUserStoreService.getJsonHour(tokenQueue.getId())).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "getQueue",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Get existing state of the queue to change the settings.
     */
    @GetMapping (
            value = "/state/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String queueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Queue state associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/state by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/state by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = queueMobileService.findByCodeQR(codeQR.getText());
            StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());

            ScheduledTaskEntity scheduledTask = null;
            if (StringUtils.isNotBlank(bizStore.getScheduledTaskId())) {
                scheduledTask = scheduledTaskManager.findOneById(bizStore.getScheduledTaskId());
            }

            return new JsonModifyQueue(
                    codeQR.getText(),
                    storeHour,
                    bizStore.getAvailableTokenCount(),
                    scheduledTask).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/state/{codeQR}",
                    "queueState",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Modifies queue settings.
     */
    @PostMapping (
        value = "/removeSchedule/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String removeSchedule(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Remove schedule from queue associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/removeSchedule by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccessWithUserLevel(qid, codeQR.getText(), UserLevelEnum.S_MANAGER)) {
            LOG.info("Un-authorized store access to /api/m/mq/removeSchedule by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            if (StringUtils.isNotBlank(bizStore.getScheduledTaskId())) {
                bizService.unsetScheduledTask(bizStore.getId());
                ScheduledTaskEntity scheduledTask = scheduledTaskManager.findOneById(bizStore.getScheduledTaskId());
                scheduledTaskManager.inActive(bizStore.getScheduledTaskId());

                Date lastPlannedRun = bizStore.getQueueHistory();
                Date now = DateUtil.dateAtTimeZone(bizStore.getTimeZone());
                if (now.before(lastPlannedRun) && now.after(DateUtil.convertToDate(scheduledTask.getFrom(), bizStore.getTimeZone()))) {
                    LOG.info("lastPlannedRun={} now={} after={}", lastPlannedRun, now, DateUtil.convertToDate(scheduledTask.getFrom(), bizStore.getTimeZone())));
                    StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());
                    queueMobileService.resetTemporarySettingsOnStoreHour(storeHour.getId());
                } else {
                    LOG.info("lastPlannedRun={} now={}", lastPlannedRun, now);
                    StoreHourEntity storeHour = queueMobileService.getQueueStateForTomorrow(codeQR.getText());
                    queueMobileService.resetTemporarySettingsOnStoreHour(storeHour.getId());
                }

                /* Send email when store setting changes. */
                UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
                String changeInitiateReason = "Removed Scheduled " + scheduledTask.getScheduleTask() + " from App, modified by " + userProfile.getEmail();
                bizService.sendMailWhenStoreSettingHasChanged(bizStore.getId(), changeInitiateReason);
            }

            StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());
            return new JsonModifyQueue(codeQR.getText(), storeHour, bizStore.getAvailableTokenCount(), null).asJson();
        } catch (Exception e) {
            LOG.error("Failed removing schedule from queues reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/removeSchedule/{codeQR}",
                "removeSchedule",
                ManageQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Modifies queue settings.
     */
    @PostMapping (
            value = "/modify",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String queueStateModify(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            JsonModifyQueue modifyQueue,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Modify queue associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(modifyQueue.getCodeQR())) {
            LOG.warn("Not a valid codeQR={} qid={}", modifyQueue.getCodeQR(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, modifyQueue.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        BizStoreEntity bizStore = bizService.findByCodeQR(modifyQueue.getCodeQR());
        if (StringUtils.isNotBlank(bizStore.getScheduledTaskId())) {
            ScheduledTaskEntity scheduledTask = scheduledTaskManager.findOneById(bizStore.getScheduledTaskId());
            Date from = DateUtil.convertToDate(scheduledTask.getFrom(), TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId());
            Date until = DateUtil.convertToDate(scheduledTask.getUntil(), TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId());
            if (DateUtil.isThisDayBetween(from, until, TODAY, TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId())) {
                return getErrorReason("Cannot modify as schedule is active. Delete set schedule to modify.", MOBILE_ACTION_NOT_PERMITTED);
            } else {
                ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId());
                LOG.info("Today={} or {} is not between From={} Until={}",
                        new Date(), Date.from(zonedDateTime.toInstant()), from, until);
            }
        }

        if (StringUtils.isNotBlank(modifyQueue.getFromDay()) || StringUtils.isNotBlank(modifyQueue.getUntilDay())) {
            if (StringUtils.isNotBlank(modifyQueue.getFromDay()) && StringUtils.isNotBlank(modifyQueue.getUntilDay())) {
                Date from = DateUtil.convertToDate(modifyQueue.getFromDay(), TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId());
                Date until = DateUtil.convertToDate(modifyQueue.getUntilDay(), TimeZone.getTimeZone(bizStore.getTimeZone()).toZoneId());
                if (from.after(until)) {
                    return getErrorReason("From Day has to before Until Day", MOBILE_JSON);
                }
            } else {
                return getErrorReason("Please provide with both the dates", MOBILE_JSON);
            }
        }

        try {
            LOG.info("Received Data for qid={} JsonModifyQueue={}", qid, modifyQueue.toString());
            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(modifyQueue.getCodeQR());
            if (tokenQueue.getLastNumber() > 0 && (modifyQueue.isDayClosed() || modifyQueue.isTempDayClosed())) {
                /* Notify everyone about day closed. */
                long notified = tokenQueueMobileService.notifyAllInQueueWhenStoreClosesForTheDay(
                        modifyQueue.getCodeQR(),
                        did.getText());

                LOG.info("Send message to {} when store is marked closed for the day queueName={} lastNumber={} queueStatus={}",
                        notified,
                        tokenQueue.getDisplayName(),
                        tokenQueue.getLastNumber(),
                        tokenQueue.getQueueStatus());

            } else if (modifyQueue.getDelayedInMinutes() > 0) {
                /* Notify everyone about delay. */
                tokenQueueMobileService.notifyAllInQueueAboutDelay(
                        modifyQueue.getCodeQR(),
                        modifyQueue.getDelayedInMinutes());

                LOG.info("Send message when queues starts late by minutes={} queueName={} lastNumber={} queueStatus={}",
                        modifyQueue.getDelayedInMinutes(),
                        tokenQueue.getDisplayName(),
                        tokenQueue.getLastNumber(),
                        tokenQueue.getQueueStatus());
            }

            ScheduledTaskEntity scheduledTask = getScheduledTaskIfAny(modifyQueue);
            StoreHourEntity storeHour = queueMobileService.updateQueueStateForToday(modifyQueue);
            queueMobileService.updateBizStoreAvailableTokenCount(modifyQueue.getAvailableTokenCount(), modifyQueue.getCodeQR());

            /* Send email when store setting changes. */
            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
            bizService.sendMailWhenStoreSettingHasChanged(
                storeHour.getBizStoreId(),
                "Modified Store Detail from App, modified by " + userProfile.getEmail());
            return new JsonModifyQueue(modifyQueue.getCodeQR(), storeHour, modifyQueue.getAvailableTokenCount(), scheduledTask).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/modify",
                    "queueStateModify",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * List all clients.
     */
    @PostMapping (
            value = "/showClients/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String showClients(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for codeQR={} request from mail={} did={} deviceType={} auth={}",
                codeQR,
                mail,
                did,
                deviceType,
                AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/showClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/showClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return queueMobileService.findAllClient(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queued clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/showClients/{codeQR}",
                    "showClients",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * List all clients from history.
     */
    @PostMapping (
        value = "/showClients/{codeQR}/historical",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String showClientsHistorical(
        @RequestHeader ("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable ("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for codeQR={} request from mail={} did={} deviceType={} auth={}",
            codeQR,
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/showClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/showClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return queueMobileService.findAllClientHistorical(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queued clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/showClients/{codeQR}",
                "showClients",
                ManageQueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Acquire specific token not in order. Send message of being served next to the owner of the token.
     */
    @PostMapping (
            value = "/acquire",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String acquire(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Acquired mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/acquire by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("qr") ? map.get("qr").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, codeQR)) {
                LOG.info("Un-authorized store access to /api/m/mq/acquire by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveTokenString = map.containsKey("t") ? map.get("t").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveTokenString)) {
                servedNumber = Integer.parseInt(serveTokenString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} qid={}", serveTokenString, codeQR, qid);
                return getErrorReason("Not a valid number.", MOBILE_JSON);
            }

            QueueStatusEnum queueStatus;
            try {
                queueStatus = map.containsKey("s") ? QueueStatusEnum.valueOf(map.get("s").getText()) : null;
                Assert.notNull(queueStatus, "Queue Status cannot be null");
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueStatus reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            String goTo = map.containsKey("g") ? map.get("g").getText() : "";
            if (StringUtils.isBlank(goTo)) {
                return getErrorReason("Counter name cannot be empty.", MOBILE_JSON);
            } else {
                if (counterNameLength < goTo.length()) {
                    return getErrorReason("Counter name cannot exceed character size of 20.", MOBILE_JSON);
                }
            }

            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(codeQR);
            LOG.info("queueStatus received={} found={}, supports only queueState=Next", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case N:
                    jsonToken = queueService.getThisAsNextInQueue(codeQR, goTo, did.getText(), servedNumber);
                    break;
                default:
                    //TODO(hth) remind apps to call state of the queue when failure is encountered as state might have changed. Update app with this state.
                    LOG.warn("Un-supported condition reached for acquiring token={} when queueState={}, supports only queueState=Next", servedNumber, tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueState " + tokenQueue.getQueueStatus().getDescription());
            }

            if (null == jsonToken) {
                LOG.warn("Failed to acquire client={} qid={} did={}", serveTokenString, qid, did);
                return getErrorReason("Could not acquire client " + serveTokenString, MERCHANT_COULD_NOT_ACQUIRE);
            }
            LOG.info("On served response servedNumber={} nowServicing={} jsonToken={}", servedNumber, jsonToken.getServingNumber(), jsonToken);
            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/acquire",
                    "acquire",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * When person walks in without phone or app. Merchant is capable of giving out token to walk-ins.
     */
    @PostMapping (
            value = "/dispenseToken/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String dispenseTokenWithoutClientInfo(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Dispense Token by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/dispenseToken/{codeQR} by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(codeQR.getText());
        if (null == bizStore) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
            return null;
        }

        try {
            return tokenQueueMobileService.joinQueue(
                    codeQR.getText(),
                    CommonUtil.appendRandomToDeviceId(did.getText()),
                    null,
                    null,
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.M).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/dispenseToken/{codeQR}",
                    "dispenseTokenWithoutClientInfo",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * When person walks in without phone or app. Merchant is capable of giving out token to walk-ins.
     */
    @PostMapping (
            value = "/dispenseToken",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String dispenseTokenWithClientInfo(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Dispense Token by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/dispenseToken by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomerLookup businessCustomerLookup = new ObjectMapper().readValue(
                    requestBodyJson,
                    JsonBusinessCustomerLookup.class);

            if (StringUtils.isBlank(businessCustomerLookup.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomerLookup.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomerLookup.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/mq/dispenseToken by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(businessCustomerLookup.getCodeQR());
            if (null == bizStore) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            UserProfileEntity userProfile = null;
            if (StringUtils.isNotBlank(businessCustomerLookup.getCustomerPhone())) {
                userProfile = accountService.checkUserExistsByPhone(businessCustomerLookup.getCustomerPhone());
            } else if (StringUtils.isNotBlank(businessCustomerLookup.getBusinessCustomerId())) {
                userProfile = businessCustomerService.findByBusinessCustomerIdAndBizNameId(
                        businessCustomerLookup.getBusinessCustomerId(),
                        bizStore.getBizName().getId());
            }

            if (null == userProfile) {
                LOG.info("Failed joining queue as no user found with phone={} businessCustomerId={}",
                        businessCustomerLookup.getCustomerPhone(),
                        businessCustomerLookup.getBusinessCustomerId());

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), businessCustomerLookup.getCustomerPhone());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            String guardianQid = null;
            if (StringUtils.isNotBlank(userProfile.getGuardianPhone())) {
                guardianQid = accountService.checkUserExistsByPhone(userProfile.getGuardianPhone()).getQueueUserId();
            }

            return tokenQueueMobileService.joinQueue(
                    businessCustomerLookup.getCodeQR(),
                    CommonUtil.appendRandomToDeviceId(did.getText()),
                    userProfile.getQueueUserId(),
                    guardianQid,
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.M).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/dispenseToken",
                    "dispenseTokenWithClientInfo",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Change the person in queue. 
     */
    @PostMapping (
            value = "/changeUserInQueue",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String changeUserInQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput deviceType,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Dispense Token by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/changeUserInQueue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            ChangeUserInQueue changeUserInQueue = new ObjectMapper().readValue(
                    requestBodyJson,
                    ChangeUserInQueue.class);

            if (StringUtils.isBlank(changeUserInQueue.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", changeUserInQueue.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, changeUserInQueue.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/mq/changeUserInQueue by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            if (!queueService.doesExistsByQid(changeUserInQueue.getCodeQR(), changeUserInQueue.getTokenNumber(), changeUserInQueue.getExistingQueueUserId())) {
                LOG.info("Un-authorized store access to /api/m/mq/changeUserInQueue by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            QueueEntity queue = queueService.changeUserInQueue(
                    changeUserInQueue.getCodeQR(),
                    changeUserInQueue.getTokenNumber(),
                    changeUserInQueue.getExistingQueueUserId(),
                    changeUserInQueue.getChangeToQueueUserId());
            tokenQueueService.updateQueueWithUserDetail(changeUserInQueue.getCodeQR(), changeUserInQueue.getChangeToQueueUserId(), queue);

            return queueService.getQueuedPerson(queue.getQueueUserId(), queue.getCodeQR());
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/dispenseToken",
                    "dispenseTokenWithClientInfo",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private ScheduledTaskEntity getScheduledTaskIfAny(JsonModifyQueue modifyQueue) {
        ScheduledTaskEntity scheduledTask = null;
        if (StringUtils.isNotBlank(modifyQueue.getFromDay()) && StringUtils.isNotBlank(modifyQueue.getUntilDay())) {
            String id = CommonUtil.generateHexFromObjectId();
            bizService.setScheduleTaskId(modifyQueue.getCodeQR(), id);

            scheduledTask = new ScheduledTaskEntity()
                .setFrom(modifyQueue.getFromDay())
                .setUntil(modifyQueue.getUntilDay())
                .setScheduleTask(ScheduleTaskEnum.CLOSE);
            scheduledTask.setId(id);
            scheduledTaskManager.save(scheduledTask);
        }

        BizStoreEntity bizStore = bizService.findByCodeQR(modifyQueue.getCodeQR());
        if (StringUtils.isNotBlank(bizStore.getScheduledTaskId())) {
            scheduledTask = scheduledTaskManager.findOneById(bizStore.getScheduledTaskId());
        }
        return scheduledTask;
    }
}
