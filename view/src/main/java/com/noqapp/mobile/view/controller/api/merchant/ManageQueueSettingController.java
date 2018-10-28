package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.common.utils.DateUtil.Day.TODAY;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_ACTION_NOT_PERMITTED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.DateUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.ScheduledTaskEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.types.ActionTypeEnum;
import com.noqapp.domain.types.ScheduleTaskEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.repository.ScheduledTaskManager;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.BizStoreElasticService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BizService;
import com.noqapp.service.BusinessUserStoreService;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 10/26/18 1:18 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/mq")
public class ManageQueueSettingController {
    private static final Logger LOG = LoggerFactory.getLogger(ManageQueueSettingController.class);

    private BizService bizService;
    private AccountService accountService;
    private QueueMobileService queueMobileService;
    private ScheduledTaskManager scheduledTaskManager;
    private AuthenticateMobileService authenticateMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private TokenQueueMobileService tokenQueueMobileService;
    private BizStoreElasticService bizStoreElasticService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ManageQueueSettingController(
        BizService bizService,
        AccountService accountService,
        QueueMobileService queueMobileService,
        ScheduledTaskManager scheduledTaskManager,
        AuthenticateMobileService authenticateMobileService,
        BusinessUserStoreService businessUserStoreService,
        TokenQueueMobileService tokenQueueMobileService,
        BizStoreElasticService bizStoreElasticService,
        ApiHealthService apiHealthService
    ) {
        this.bizService = bizService;
        this.accountService = accountService;
        this.queueMobileService = queueMobileService;
        this.scheduledTaskManager = scheduledTaskManager;
        this.authenticateMobileService = authenticateMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.bizStoreElasticService = bizStoreElasticService;
        this.apiHealthService = apiHealthService;
    }

    /** Get existing state of the queue to change the settings. */
    @GetMapping(
        value = "/state/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String queueState(
        @RequestHeader("X-R-DID")
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
                bizStore.isActive() ? ActionTypeEnum.ACTIVE : ActionTypeEnum.INACTIVE,
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

    /** Remove schedule. */
    @PostMapping(
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
                ScheduledTaskEntity scheduledTask = scheduledTaskManager.findOneById(bizStore.getScheduledTaskId());
                Date lastPlannedRun = bizStore.getQueueHistory();
                Date now = DateUtil.dateAtTimeZone(bizStore.getTimeZone());
                /* This condition is when schedule is active. */
                if (now.before(lastPlannedRun) && now.after(DateUtil.convertToDate(scheduledTask.getFrom(), bizStore.getTimeZone()))) {
                    LOG.info("lastPlannedRun={} now={} after={}", lastPlannedRun, now, DateUtil.convertToDate(scheduledTask.getFrom(), bizStore.getTimeZone()));
                    StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());
                    queueMobileService.resetTemporarySettingsOnStoreHour(storeHour.getId());
                } else {
                    /* Otherwise, work on resetting tomorrow's store schedule and reset today's run. */
                    LOG.info("lastPlannedRun={} now={}", lastPlannedRun, now);
                    StoreHourEntity storeHour = queueMobileService.getQueueStateForTomorrow(codeQR.getText());
                    queueMobileService.resetTemporarySettingsOnStoreHour(storeHour.getId());
                    queueMobileService.updateNextRun(bizStore, storeHour);
                }
                bizService.unsetScheduledTask(bizStore.getId());
                scheduledTaskManager.inActive(bizStore.getScheduledTaskId());

                /* Send email when store setting changes. */
                String changeInitiateReason = "Removed Scheduled " + scheduledTask.getScheduleTask() + " from App, modified by " + accountService.findProfileByQueueUserId(qid).getEmail();
                bizService.sendMailWhenStoreSettingHasChanged(bizStore.getId(), changeInitiateReason);
            }

            StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());
            return new JsonModifyQueue(
                codeQR.getText(),
                storeHour,
                bizStore.getAvailableTokenCount(),
                bizStore.isActive() ? ActionTypeEnum.ACTIVE : ActionTypeEnum.INACTIVE,
                null).asJson();
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

    /** Modifies queue settings. */
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
                LOG.info("Today={} or {} is not between From={} Until={}", new Date(), Date.from(zonedDateTime.toInstant()), from, until);
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

            /* Store Offline or Online based on ActionType. */
            if (null != modifyQueue.getStoreActionType()) {
                boolean active = bizService.activeInActiveStore(storeHour.getBizStoreId(), modifyQueue.getStoreActionType());
                if (active) {
                    bizStore.active();
                } else {
                    bizStore.inActive();
                }
            }
            updateChangesMadeOnElastic(bizStore);

            /* Send email when store setting changes. */
            String changeInitiateReason = "Modified Store Detail from App, modified by " +  accountService.findProfileByQueueUserId(qid).getEmail();
            bizService.sendMailWhenStoreSettingHasChanged(storeHour.getBizStoreId(), changeInitiateReason);

            return new JsonModifyQueue(
                modifyQueue.getCodeQR(),
                storeHour,
                modifyQueue.getAvailableTokenCount(),
                bizStore.isActive() ? ActionTypeEnum.ACTIVE : ActionTypeEnum.INACTIVE,
                scheduledTask).asJson();
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

    private void updateChangesMadeOnElastic(BizStoreEntity bizStore) {
        if (bizStore.isActive()) {
            bizStoreElasticService.save(DomainConversion.getAsBizStoreElastic(bizStore, bizService.findAllStoreHours(bizStore.getId())));
        } else {
            bizStoreElasticService.delete(bizStore.getId());
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
