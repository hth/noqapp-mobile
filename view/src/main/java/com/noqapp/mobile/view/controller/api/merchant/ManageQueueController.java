package com.noqapp.mobile.view.controller.api.merchant;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ParseJsonStringToMap;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.domain.types.TokenServiceEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.service.BusinessUserStoreService;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MERCHANT_COULD_NOT_ACQUIRE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

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
    private QueueMobileService queueMobileService;
    private BusinessUserStoreService businessUserStoreService;
    private TokenQueueMobileService tokenQueueMobileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ManageQueueController(
            @Value("${ManageQueueController.counterNameLength}")
            int counterNameLength,

            AuthenticateMobileService authenticateMobileService,
            QueueMobileService queueMobileService,
            BusinessUserStoreService businessUserStoreService,
            TokenQueueMobileService tokenQueueMobileService,
            ApiHealthService apiHealthService
    ) {
        this.counterNameLength = counterNameLength;
        this.authenticateMobileService = authenticateMobileService;
        this.queueMobileService = queueMobileService;
        this.businessUserStoreService = businessUserStoreService;
        this.tokenQueueMobileService = tokenQueueMobileService;
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
            apiHealthService.insert(
                    "/queues",
                    "getQueues",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queues",
                    "getQueues",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
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
            String codeQR = map.containsKey("c") ? map.get("c").getText() : null;

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
                servedNumber = Integer.valueOf(serveTokenString);
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

            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(codeQR);
            LOG.info("queueStatus received={} found={}", queueStatus, tokenQueue.getQueueStatus());

            JsonToken jsonToken;
            switch (tokenQueue.getQueueStatus()) {
                case C:
                case D:
                case N:
                    if (queueStatus == QueueStatusEnum.P) {
                        if (queueUserState == QueueUserStateEnum.S) {
                            jsonToken = queueMobileService.pauseServingQueue(codeQR, servedNumber, queueUserState, did.getText());
                        } else {
                            return getErrorReason("Cannot pause until the last person has been served", MOBILE);
                        }
                    } else {
                        jsonToken = queueMobileService.updateAndGetNextInQueue(codeQR, servedNumber, queueUserState, goTo, did.getText());
                    }
                    break;
                case R:
                case S:
                    jsonToken = queueMobileService.getNextInQueue(codeQR, goTo, did.getText());
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
            apiHealthService.insert(
                    "/served",
                    "served",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/served",
                    "served",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
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
            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(codeQR.getText());
            if (null == tokenQueue) {
                LOG.error("Failed finding codeQR={} by mail={}", codeQR.getText(), mail);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            return new JsonTopic(tokenQueue).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queue reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "getQueue",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/queue/{codeQR}",
                    "getQueue",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
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

            return new JsonModifyQueue(
                    codeQR.getText(),
                    storeHour,
                    bizStore.getAvailableTokenCount()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/state/{codeQR}",
                    "queueState",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/state/{codeQR}",
                    "queueState",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * Modifies the state of queue.
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
            JsonModifyQueue requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Modify queue associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(requestBodyJson.getCodeQR())) {
            LOG.warn("Not a valid codeQR={} qid={}", requestBodyJson.getCodeQR(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, requestBodyJson.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            LOG.info("Received Data for JsonModifyQueue={}", requestBodyJson.toString());
            TokenQueueEntity tokenQueue = queueMobileService.getTokenQueueByCodeQR(requestBodyJson.getCodeQR());
            if (tokenQueue.getLastNumber() > 0 && requestBodyJson.isDayClosed()) {
                /* Notify everyone about day closed. */
                tokenQueueMobileService.notifyAllInQueueWhenStoreClosesForTheDay(requestBodyJson.getCodeQR());
            }

            StoreHourEntity storeHour = queueMobileService.updateQueueStateForToday(
                    requestBodyJson.getCodeQR(),
                    requestBodyJson.getTokenAvailableFrom(),
                    requestBodyJson.getStartHour(),
                    requestBodyJson.getTokenNotAvailableFrom(),
                    requestBodyJson.getEndHour(),
                    requestBodyJson.isPreventJoining(),
                    requestBodyJson.isDayClosed(),
                    requestBodyJson.getDelayedInMinutes());

            //TODO add missing available token count to iOS and Android.
            queueMobileService.updateBizStoreAvailableTokenCount(
                    requestBodyJson.getAvailableTokenCount(),
                    requestBodyJson.getCodeQR());

            return new JsonModifyQueue(
                    requestBodyJson.getCodeQR(),
                    storeHour,
                    requestBodyJson.getAvailableTokenCount()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/modify",
                    "queueStateModify",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/modify",
                    "queueStateModify",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * List all the queued clients.
     */
    @PostMapping (
            value = "/showQueuedClients/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String showQueuedClients(
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
        Instant start = Instant.now();
        LOG.info("Queued Clients shown for codeQR={} request from mail={} did={} deviceType={} auth={}",
                codeQR,
                mail,
                did,
                deviceType,
                AUTH_KEY_HIDDEN);

        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/showQueuedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} qid={}", codeQR.getText(), qid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(qid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/showQueuedClients by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return queueMobileService.findAllClientQueuedOrAborted(codeQR.getText()).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queued clients reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/showQueuedClients/{codeQR}",
                    "showQueuedClients",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/showQueuedClients/{codeQR}",
                    "showQueuedClients",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
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
            String codeQR = map.containsKey("c") ? map.get("c").getText() : null;

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
                servedNumber = Integer.valueOf(serveTokenString);
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
                    jsonToken = queueMobileService.getThisAsNextInQueue(codeQR, goTo, did.getText(), servedNumber);
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
            apiHealthService.insert(
                    "/acquire",
                    "acquire",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/acquire",
                    "acquire",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }

    /**
     * When person walks in without phone or app. Merchant is capable of giving out token to walk-ins.
     */
    @PostMapping (
            value = "/dispenseToken/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String dispenseToken(
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
        Instant start = Instant.now();
        LOG.info("Dispense Token by mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/mq/dispenseToken by mail={}", mail);
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
                    bizStore.getAverageServiceTime(),
                    TokenServiceEnum.M).asJson();
        } catch (Exception e) {
            LOG.error("Failed joining queue qid={}, reason={}", qid, e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/dispenseToken/{codeQR}",
                    "dispenseToken",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/dispenseToken/{codeQR}",
                    "dispenseToken",
                    ManageQueueController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
