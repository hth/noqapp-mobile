package com.noqapp.mobile.view.controller.api.merchant.queue;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.CHANGE_USER_IN_QUEUE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.DEVICE_TIMEZONE_OFF;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOINING_NOT_PRE_APPROVED_QUEUE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOINING_QUEUE_PERMISSION_DENIED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.JOIN_PRE_APPROVED_QUEUE_ONLY;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MERCHANT_COULD_NOT_ACQUIRE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.ORDER_PAYMENT_PAID_ALREADY_FAILED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.ORDER_PAYMENT_UPDATE_FAILED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_ALREADY_CANCELLED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.PURCHASE_ORDER_FAILED_TO_CANCEL;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_NOT_RE_STARTED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_NOT_STARTED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_NO_SERVICE_NO_PAY;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_SERVICE_LIMIT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.QUEUE_TOKEN_LIMIT;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SERVICED_TODAY;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SERVICE_AFTER_CLOSING_HOUR;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.STORE_DAY_CLOSED;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_ALREADY_IN_QUEUE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.WAIT_UNTIL_SERVICE_BEGUN;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.RegisteredDeviceEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonPurchaseOrderList;
import com.noqapp.domain.json.JsonQueuedPerson;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.BusinessTypeEnum;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.domain.types.TransactionViaEnum;
import com.noqapp.domain.types.UserLevelEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.json.JsonMedicalRecord;
import com.noqapp.medical.service.MedicalRecordService;
import com.noqapp.mobile.domain.body.merchant.ChangeUserInQueue;
import com.noqapp.mobile.domain.body.merchant.CodeQRDateRangeLookup;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.MerchantExtendingJoinService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.mobile.view.controller.api.merchant.store.PurchaseOrderController;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.DeviceService;
import com.noqapp.service.JoinAbortService;
import com.noqapp.service.NotifyMobileService;
import com.noqapp.service.PurchaseOrderService;
import com.noqapp.service.QueueService;
import com.noqapp.service.TokenQueueService;
import com.noqapp.service.exceptions.AlreadyServicedTodayException;
import com.noqapp.service.exceptions.BeforeStartOfStoreException;
import com.noqapp.service.exceptions.ExpectedServiceBeyondStoreClosingHour;
import com.noqapp.service.exceptions.JoiningNonApprovedQueueException;
import com.noqapp.service.exceptions.JoiningQueuePermissionDeniedException;
import com.noqapp.service.exceptions.JoiningQueuePreApprovedRequiredException;
import com.noqapp.service.exceptions.LimitedPeriodException;
import com.noqapp.service.exceptions.StoreDayClosedException;
import com.noqapp.service.exceptions.TokenAvailableLimitReachedException;
import com.noqapp.service.exceptions.WaitUntilServiceBegunException;

import com.fasterxml.jackson.databind.JsonMappingException;

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
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletResponse;

/**
 * Managed by merchant
 * User: hitender
 * Date: 1/9/17 10:15 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/q")
public class QueueController {
    private static final Logger LOG = LoggerFactory.getLogger(QueueController.class);

    private int counterNameLength;
    private AuthenticateMobileService authenticateMobileService;
    private QueueService queueService;
    private QueueMobileService queueMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private TokenQueueService tokenQueueService;
    private TokenQueueMobileService tokenQueueMobileService;
    private JoinAbortService joinAbortService;
    private PurchaseOrderService purchaseOrderService;
    private MedicalRecordService medicalRecordService;
    private DeviceService deviceService;
    private MerchantExtendingJoinService merchantExtendingJoinService;
    private NotifyMobileService notifyMobileService;
    private ApiHealthService apiHealthService;

    private ExecutorService executorService;

    @Autowired
    public QueueController(
        @Value("${ManageQueueController.counterNameLength}")
        int counterNameLength,

        AuthenticateMobileService authenticateMobileService,
        QueueService queueService,
        QueueMobileService queueMobileService,
        BusinessUserStoreService businessUserStoreService,
        TokenQueueService tokenQueueService,
        TokenQueueMobileService tokenQueueMobileService,
        JoinAbortService joinAbortService,
        PurchaseOrderService purchaseOrderService,
        MedicalRecordService medicalRecordService,
        DeviceService deviceService,
        MerchantExtendingJoinService merchantExtendingJoinService,
        NotifyMobileService notifyMobileService,
        ApiHealthService apiHealthService
    ) {
        this.counterNameLength = counterNameLength;
        this.authenticateMobileService = authenticateMobileService;
        this.queueService = queueService;
        this.queueMobileService = queueMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.tokenQueueService = tokenQueueService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.joinAbortService = joinAbortService;
        this.purchaseOrderService = purchaseOrderService;
        this.medicalRecordService = medicalRecordService;
        this.deviceService = deviceService;
        this.merchantExtendingJoinService = merchantExtendingJoinService;
        this.notifyMobileService = notifyMobileService;
        this.apiHealthService = apiHealthService;

        /* For executing in order of sequence. */
        this.executorService = newSingleThreadExecutor();
    }

    @GetMapping(
        value = "/queues",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getQueues(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/q/queues by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonTopicList topics = new JsonTopicList();
            topics.setTopics(businessUserStoreService.getAssignedTokenAndQueues(qid));
            return topics.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/queues",
                "getQueues",
                QueueController.class.getName(),
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
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String served(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
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
            LOG.warn("Un-authorized access to /api/m/q/served by mail={}", mail);
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
                LOG.info("Un-authorized store access to /api/m/q/served by mail={}", mail);
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
                    LOG.info("Resetting queue when queueStatus={}", tokenQueue.getQueueStatus());
                    tokenQueueService.resetQueueSettingWhenQueueStarts(codeQR);
                    break;
                default:
                    LOG.error("Reached unsupported condition queueStatus={}", tokenQueue.getQueueStatus());
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
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Most called during refresh or reload of the app.
     */
    @GetMapping(
        value = "/queue/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String getQueue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
        ScrubbedInput codeQR,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Single queue associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/q/queue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR, qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/q/queue by mail={}", mail);
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
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * List all clients in queue.
     */
    @PostMapping(
        value = "/showClients/{codeQR}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String showClients(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @PathVariable("codeQR")
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
            LOG.warn("Un-authorized access to /api/m/q/showClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/q/showClients by mail={}", mail);
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
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** List all registered clients from history within the specified date range. Non registered clients are never shown. */
    @PostMapping(
        value = "/showClients/historical",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String showClientsHistorical(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        CodeQRDateRangeLookup codeQRDateRangeLookup,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Clients shown for {} request from mail={} did={} deviceType={} auth={}",
            codeQRDateRangeLookup,
            mail,
            did,
            deviceType,
            AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/q/showClients/{codeQR}/historical by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQRDateRangeLookup.getCodeQR().getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQRDateRangeLookup.getCodeQR().getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQRDateRangeLookup.getCodeQR().getText())) {
            LOG.info("Un-authorized store access to /api/m/q/showClients/{codeQR}/historical by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            BizStoreEntity bizStore = queueMobileService.findByCodeQR(codeQRDateRangeLookup.getCodeQR().getText());
            LocalDate fromLocalDate = LocalDate.parse(codeQRDateRangeLookup.getFrom().getText());
            Date fromDate = Date.from(fromLocalDate.atStartOfDay(ZoneId.of(bizStore.getTimeZone())).toInstant());

            LocalDate untilLocalDate = LocalDate.parse(codeQRDateRangeLookup.getUntil().getText());
            Date untilDate = Date.from(untilLocalDate.atStartOfDay(ZoneId.of(bizStore.getTimeZone())).toInstant());

//            if (DateUtil.getDaysBetween(fromDate, untilDate) > 30) {
//                LOG.warn("Greater than 30 days. Limiting to 30 days {}", codeQRDateRangeLookup);
//                untilDate = DateUtil.asDate(fromLocalDate.plusDays(30));
//            }
            return queueMobileService.findAllRegisteredClientHistorical(codeQRDateRangeLookup.getCodeQR().getText(),fromDate, untilDate).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queued clients reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/showClients/{codeQR}/historical",
                "showClientsHistorical",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Acquire specific token not in order. Send message of being served next to the owner of the token. */
    @PostMapping(
        value = "/acquire",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String acquire(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
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
            LOG.warn("Un-authorized access to /api/m/q/acquire by mail={}", mail);
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
                LOG.info("Un-authorized store access to /api/m/q/acquire by mail={}", mail);
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
            LOG.info("queueStatus received={} found={}, supports only queueStatus=Next", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case N:
                    jsonToken = queueService.getThisAsNextInQueue(codeQR, goTo, did.getText(), servedNumber);
                    break;
                case S:
                    LOG.info("Queue has not started qid={} queueStatus={} displayName={}", qid, queueStatus, tokenQueue.getDisplayName());
                    return getErrorReason("Click start button to begin", QUEUE_NOT_STARTED);
                case R:
                    LOG.info("Queue has not re-started qid={} queueStatus={} displayName={}", qid, queueStatus, tokenQueue.getDisplayName());
                    return getErrorReason("Click continue button to begin", QUEUE_NOT_RE_STARTED);
                default:
                    //TODO(hth) remind apps to call state of the queue when failure is encountered as state might have changed. Update app with this state.
                    LOG.error("Un-supported condition reached for acquiring token={} when queueStatus={}, supports only queueStatus=Next", servedNumber, tokenQueue.getQueueStatus());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueStatus " + tokenQueue.getQueueStatus().getDescription());
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
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When person walks in without phone or app. Merchant is capable of giving out token to walk-ins. */
    @PostMapping(
        value = "/dispenseToken",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String dispenseTokenWithClientInfo(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonBusinessCustomer businessCustomer,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Dispense Token by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/q/dispenseToken by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(businessCustomer.getCodeQR().getText())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomer.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomer.getCodeQR().getText())) {
                LOG.info("Un-authorized store access to /api/m/q/dispenseToken by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BizStoreEntity bizStore = tokenQueueMobileService.getBizService().findByCodeQR(businessCustomer.getCodeQR().getText());
            if (null == bizStore) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid QR Code");
                return null;
            }

            try {
                switch (bizStore.getBusinessType()) {
                    case CD:
                    case CDQ:
                        BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, businessCustomer.getCodeQR().getText());
                        if (UserLevelEnum.S_MANAGER == businessUserStore.getUserLevel()) {
                            return merchantExtendingJoinService.dispenseTokenWithClientInfo(did.getText(), businessCustomer, bizStore);
                        }
                        throw new TokenAvailableLimitReachedException("Not authorized to create token");
                    default:
                        return merchantExtendingJoinService.dispenseTokenWithClientInfo(did.getText(), businessCustomer, bizStore);
                }
            } catch (StoreDayClosedException e) {
                LOG.warn("Failed joining queue store closed qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = false;
                return ErrorEncounteredJson.toJson("Store is closed today", STORE_DAY_CLOSED);
            } catch (BeforeStartOfStoreException e) {
                LOG.warn("Failed joining queue as trying to join before store opens qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " has not started. Please correct time on your device.", DEVICE_TIMEZONE_OFF);
            } catch (AlreadyServicedTodayException e) {
                LOG.warn("Failed joining queue as have been serviced or skipped today qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " has either serviced or skipped you for today. Try another day for service", SERVICED_TODAY);
            } catch (WaitUntilServiceBegunException e) {
                LOG.warn("Failed joining queue as you have cancelled service today qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson("Cancelled service. Please wait until service has begun to reclaim your spot if available.", WAIT_UNTIL_SERVICE_BEGUN);
            } catch (ExpectedServiceBeyondStoreClosingHour e) {
                LOG.warn("Failed joining queue as service time is after store close qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " will close by the time you receive service. Please do not visit.", SERVICE_AFTER_CLOSING_HOUR);
            } catch (LimitedPeriodException e) {
                LOG.warn("Failed joining queue as limited join allowed qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                String message = bizStore.getDisplayName() + " allows a customer one token in " + bizStore.getBizName().getLimitServiceByDays()
                    + " days. You have been serviced with-in past " + bizStore.getBizName().getLimitServiceByDays()
                    + " days. Please try again later.";
                return ErrorEncounteredJson.toJson(message, QUEUE_SERVICE_LIMIT);
            } catch (TokenAvailableLimitReachedException e) {
                LOG.warn("Failed joining queue as token limit reached qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson(bizStore.getDisplayName() + " token limit for the day has reached.", QUEUE_TOKEN_LIMIT);
            } catch (JoiningQueuePreApprovedRequiredException e) {
                LOG.warn("Store has to pre-approve qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson("Store has to pre-approve. Please complete pre-approval before joining the queue.", JOIN_PRE_APPROVED_QUEUE_ONLY);
            } catch (JoiningNonApprovedQueueException e) {
                LOG.warn("This queue is not approved qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson("This queue is not approved. Select correct pre-approved queue.", JOINING_NOT_PRE_APPROVED_QUEUE);
            } catch (JoiningQueuePermissionDeniedException e) {
                LOG.warn("Store prevented user from joining queue qid={}, reason={}", qid, e.getLocalizedMessage());
                methodStatusSuccess = true;
                return ErrorEncounteredJson.toJson("Store has denied you from joining the queue. Please contact store for resolving this issue.", JOINING_QUEUE_PERMISSION_DENIED);
            }
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/dispenseToken",
                "dispenseTokenWithClientInfo",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Change the person in queue.
     */
    @PostMapping(
        value = "/changeUserInQueue",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String changeUserInQueue(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        ChangeUserInQueue changeUserInQueue,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Change user by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/q/changeUserInQueue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (StringUtils.isBlank(changeUserInQueue.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", changeUserInQueue.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, changeUserInQueue.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/q/changeUserInQueue by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            if (!queueService.doesExistsByQid(changeUserInQueue.getCodeQR(), changeUserInQueue.getTokenNumber(), changeUserInQueue.getExistingQueueUserId())) {
                LOG.info("Un-authorized store access to /api/m/q/changeUserInQueue by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            QueueEntity existingInQueue = queueService.findQueuedOneByQid(changeUserInQueue.getCodeQR(), changeUserInQueue.getChangeToQueueUserId());
            if (null != existingInQueue) {
                LOG.warn("User already in queue qid={}", changeUserInQueue.getChangeToQueueUserId());
                return getErrorReason("User already in queue", USER_ALREADY_IN_QUEUE);
            }

            QueueEntity queue = null;
            try {
                queue = queueService.changeUserInQueue(
                        changeUserInQueue.getCodeQR(),
                        changeUserInQueue.getTokenNumber(),
                        changeUserInQueue.getExistingQueueUserId(),
                        changeUserInQueue.getChangeToQueueUserId());

                /* Update changes in medical record. */
                if (BusinessTypeEnum.DO == queue.getBusinessType()) {
                    JsonMedicalRecord jsonMedicalRecord = medicalRecordService.findMedicalRecord(queue.getCodeQR(), queue.getRecordReferenceId());
                    if (null != jsonMedicalRecord) {
                        jsonMedicalRecord.setQueueUserId(changeUserInQueue.getChangeToQueueUserId());
                        medicalRecordService.changePatient(jsonMedicalRecord, qid);
                    }
                }

                /* Updated with user details. */
                tokenQueueService.updateQueueWithUserDetail(changeUserInQueue.getCodeQR(), changeUserInQueue.getChangeToQueueUserId(), queue);
                return queueService.getQueuedPerson(queue.getQueueUserId(), queue.getCodeQR());
            } catch (Exception e) {
                LOG.error("Failed changing user, reverting {}", e.getLocalizedMessage(), e);
                if (queue != null) {
                    LOG.warn("Reverting changes to original");
                    queueService.changeUserInQueue(
                            changeUserInQueue.getCodeQR(),
                            changeUserInQueue.getTokenNumber(),
                            changeUserInQueue.getChangeToQueueUserId(),
                            changeUserInQueue.getExistingQueueUserId());

                    /* Send alert on change user failed. */
                    return getErrorReason("Change user is not permitted", CHANGE_USER_IN_QUEUE);
                }
            }

            /* Should not reach here. */
            throw new RuntimeException("Reached Un-reachable condition");
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/changeUserInQueue",
                "changeUserInQueue",
                QueueController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** When payment is performed at counter via external means. */
    @PostMapping(
        value = "/counterPayment",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String counterPayment(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonQueuedPerson jsonQueuedPerson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Purchase Order Cash Payment API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/q/counterPayment")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/q/counterPayment by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            QueueEntity queue = queueService.findByTransactionId(
                jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR(),
                jsonQueuedPerson.getJsonPurchaseOrder().getTransactionId(),
                jsonQueuedPerson.getQueueUserId());

            if (null == queue) {
                LOG.error("Not found queue for {} {}", jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR(), jsonQueuedPerson.getQueueUserId());
                return jsonQueuedPerson.asJson();
            }

            /* Let there be some validation before sending this message out when person is about to override existing payment. */
            if (purchaseOrderService.isPaid(jsonQueuedPerson.getTransactionId())) {
                LOG.warn("Cannot accept payment when already accepted {} {} ", jsonQueuedPerson.getTransactionId(), qid);
                return ErrorEncounteredJson.toJson("Cannot accept payment when already accepted", ORDER_PAYMENT_PAID_ALREADY_FAILED);
            }

            switch (queue.getQueueUserState()) {
                case I:
                case N:
                case A:
                    LOG.error("Trying to accept payment on non serviced by qid={} for {} {}", qid, queue.getTransactionId(), queue.getQueueUserId());
                    return ErrorEncounteredJson.toJson("Cannot accept payment when not serviced", QUEUE_NO_SERVICE_NO_PAY);
            }

            JsonPurchaseOrder jsonPurchaseOrder = purchaseOrderService.counterPayment(jsonQueuedPerson.getJsonPurchaseOrder(), qid);
            JsonQueuedPerson jsonQueuedPersonUpdated = queueService.getJsonQueuedPerson(queue);
            jsonQueuedPersonUpdated.setJsonPurchaseOrder(jsonPurchaseOrder);
            LOG.info("Order counter payment updated successfully={}", jsonPurchaseOrder);

            /* Send notification to all merchant. As there can be multiple merchants that needs notification for update. */
            executorService.execute(() -> tokenQueueService.forceRefreshOnSomeActivity(jsonPurchaseOrder.getCodeQR()));

            RegisteredDeviceEntity registeredDevice = deviceService.findByDid(jsonPurchaseOrder.getDid());
            if (null != registeredDevice) {
                notifyMobileService.notifyClient(registeredDevice,
                    "Paid at counter for " + queue.getDisplayName(),
                    "Your order " + jsonPurchaseOrder.getDisplayToken() + " at " + queue.getDisplayName()
                        + " has been paid at the counter via " + jsonPurchaseOrder.getPaymentMode().getDescription(),
                    jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR());
            }

            return jsonQueuedPersonUpdated.asJson();
        } catch (Exception e) {
            LOG.error("Failed processing cash payment on order reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return ErrorEncounteredJson.toJson("Failed Updating Order Payment", ORDER_PAYMENT_UPDATE_FAILED);
        } finally {
            apiHealthService.insert(
                "/counterPayment",
                "counterPayment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /**
     * Cancel placed order. This initiates a refund process.
     * Note: Merchant get two notification as it is sent by purchaseOrder when cancelled and the other one is due to Queue Aborted. Where as
     * client receives one personal message on refund that mentions refund has been given at counter. Client message has to be saved in
     * notification.
     */
    @PostMapping(
        value = "/cancel",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String cancel(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonQueuedPerson jsonQueuedPerson,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Cancel order mail={} did={} deviceType={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid, mail.getText(), did.getText(), "/api/m/q/cancel")) return null;

        if (!businessUserStoreService.hasAccess(qid, jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/q/cancel by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            if (purchaseOrderService.isOrderCancelled(jsonQueuedPerson.getJsonPurchaseOrder().getQueueUserId(), jsonQueuedPerson.getJsonPurchaseOrder().getTransactionId())) {
                LOG.error("Cancel order fail {} {}", jsonQueuedPerson.getJsonPurchaseOrder().getQueueUserId(), jsonQueuedPerson.getJsonPurchaseOrder().getTransactionId());
                return getErrorReason("Order already cancelled", PURCHASE_ORDER_ALREADY_CANCELLED);
            }

            JsonPurchaseOrderList jsonPurchaseOrderList = purchaseOrderService.cancelOrderByMerchant(jsonQueuedPerson.getJsonPurchaseOrder().getQueueUserId(), jsonQueuedPerson.getJsonPurchaseOrder().getTransactionId());
            LOG.info("Order Cancelled Successfully={}", jsonPurchaseOrderList.getPurchaseOrders().get(0).getPresentOrderState());

            QueueEntity queue = queueService.findByTransactionId(
                jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR(),
                jsonQueuedPerson.getJsonPurchaseOrder().getTransactionId(),
                jsonQueuedPerson.getQueueUserId());

            /* Abort when Refund is initiated by merchant. */
            switch (queue.getQueueUserState()) {
                case Q:
                    joinAbortService.abort(queue.getId(), queue.getCodeQR());
                    break;
                case I:
                case A:
                case N:
                    break;
                default:
                    LOG.error("Reached unsupported lab category {} transactionId={}", queue.getQueueUserState(), queue.getTransactionId());
            }

            RegisteredDeviceEntity registeredDevice = deviceService.findRegisteredDeviceByQid(jsonQueuedPerson.getJsonPurchaseOrder().getQueueUserId());
            if (null != registeredDevice) {
                JsonPurchaseOrder jsonPurchaseOrderUpdated = jsonPurchaseOrderList.getPurchaseOrders().get(0);
                BizStoreEntity bizStore = queueMobileService.findByCodeQR(jsonPurchaseOrderUpdated.getCodeQR());
                String title, body;
                if (new BigDecimal(jsonPurchaseOrderUpdated.getOrderPriceForDisplay()).intValue() > 0) {
                    title = "Refund initiated by " + queue.getDisplayName();
                    body = "You have been refunded net total of " + CommonUtil.displayWithCurrencyCode(jsonPurchaseOrderUpdated.getOrderPriceForDisplay(), bizStore.getCountryShortName())
                        + (jsonPurchaseOrderUpdated.getTransactionVia() == TransactionViaEnum.I
                        ? " via " + jsonPurchaseOrderUpdated.getPaymentMode().getDescription() + ".\n\n" + "Note: It takes 7 to 10 business days for this amount to show up in your account."
                        : " at counter");
                } else {
                    title = "Cancelled order by "  + queue.getDisplayName();
                    body = "Your order " + jsonPurchaseOrderUpdated.getDisplayToken() + " was cancelled.";
                }

                notifyMobileService.notifyClient(registeredDevice,
                    title,
                    body,
                    jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR());
            }

            JsonQueuedPerson jsonQueuedPersonUpdated = queueService.getJsonQueuedPerson(queue);
            jsonQueuedPersonUpdated.setJsonPurchaseOrder(jsonPurchaseOrderList.getPurchaseOrders().get(0));
            return jsonQueuedPersonUpdated.asJson();
        } catch (Exception e) {
            LOG.error("Failed cancelling purchase order orderNumber={} codeQR={} purchaseOrderState={} reason={}",
                jsonQueuedPerson.getJsonPurchaseOrder().getToken(),
                jsonQueuedPerson.getJsonPurchaseOrder().getCodeQR(),
                jsonQueuedPerson.getJsonPurchaseOrder().getPresentOrderState(),
                e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Failed to cancel order", PURCHASE_ORDER_FAILED_TO_CANCEL);
        } finally {
            apiHealthService.insert(
                "/cancel",
                "cancel",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
