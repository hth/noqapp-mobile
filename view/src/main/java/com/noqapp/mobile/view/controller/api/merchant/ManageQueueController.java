package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.TokenQueueEntity;
import com.noqapp.domain.json.JsonToken;
import com.noqapp.domain.json.JsonTopic;
import com.noqapp.domain.json.JsonTopicList;
import com.noqapp.domain.types.QueueStatusEnum;
import com.noqapp.domain.types.QueueUserStateEnum;
import com.noqapp.mobile.domain.JsonModifyQueue;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.utils.ParseJsonStringToMap;
import com.noqapp.utils.ScrubbedInput;

import java.io.IOException;
import java.util.Map;

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

    public static final String AUTH_KEY_HIDDEN = "*********";
    public static final String UNAUTHORIZED = "Unauthorized";

    private int counterNameLength;
    private AuthenticateMobileService authenticateMobileService;
    private QueueMobileService queueMobileService;
    private BusinessUserStoreService businessUserStoreService;

    @Autowired
    public ManageQueueController(
            @Value ("${ManageQueueController.counterNameLength}")
            int counterNameLength,

            AuthenticateMobileService authenticateMobileService,
            QueueMobileService queueMobileService,
            BusinessUserStoreService businessUserStoreService
    ) {
        this.counterNameLength = counterNameLength;
        this.authenticateMobileService = authenticateMobileService;
        this.queueMobileService = queueMobileService;
        this.businessUserStoreService = businessUserStoreService;
    }

    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/queues",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueues(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("All queues associated with mail={} did={} dt={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/m/mq/queues by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonTopicList topics = new JsonTopicList();
            topics.setTopics(businessUserStoreService.getQueues(rid));
            return topics.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }

    /**
     * When client is served by merchant.
     * And
     * When client starts to serve for first time or re-start after serving the last in the queue.
     *
     * @param did
     * @param dt
     * @param mail
     * @param requestBodyJson
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/served",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String served(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Served mail={} did={} dt={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/m/mq/served by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("c") ? map.get("c").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} rid={}", codeQR, rid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(rid, codeQR)) {
                LOG.info("Un-authorized store access to /api/m/mq/served by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveTokenString = map.containsKey("t") ? map.get("t").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveTokenString)) {
                servedNumber = Integer.valueOf(serveTokenString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} rid={}", serveTokenString, codeQR, rid);
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
                    LOG.error("Reached unsupported condition queueState={}", map.get("s").getText());
                    throw new UnsupportedOperationException("Reached unsupported condition for QueueState " + map.get("s").getText());
            }

            if (null == jsonToken) {
                LOG.error("Could not find queue codeQR={} servedNumber={} queueUserState={}", codeQR, servedNumber, queueUserState);
                return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
            }

            LOG.info("On served response servedNumber={} nowServicing={} jsonToken={}", servedNumber, jsonToken.getServingNumber(), jsonToken);
            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} rid={} message={}", requestBodyJson, rid, e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }

    /**
     * Most called during refresh or reload of the app.
     * 
     * @param did
     * @param dt
     * @param mail
     * @param auth
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/queue/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String getQueue(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Single queue associated with mail={} did={} dt={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/m/mq/queue by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} rid={}", codeQR, rid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(rid, codeQR.getText())) {
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
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }

    /**
     * Get existing state of the queue to change the settings.
     *
     * @param did
     * @param dt
     * @param mail
     * @param auth
     * @param codeQR
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.GET,
            value = "/state/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String queueState(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @PathVariable ("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Queue state associated with mail={} did={} dt={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/m/mq/state by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(codeQR.getText())) {
            LOG.warn("Not a valid codeQR={} rid={}", codeQR.getText(), rid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(rid, codeQR.getText())) {
            LOG.info("Un-authorized store access to /api/m/mq/state by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            StoreHourEntity storeHour = queueMobileService.getQueueStateForToday(codeQR.getText());
            return new JsonModifyQueue(codeQR.getText(), storeHour).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }

    /**
     * Modifies the state of queue.
     *
     * @param did
     * @param dt
     * @param mail
     * @param auth
     * @param requestBodyJson
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/modify",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String queueModify(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            JsonModifyQueue requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("Modify queue associated with mail={} did={} dt={} auth={}", mail, did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (StringUtils.isBlank(requestBodyJson.getCodeQR())) {
            LOG.warn("Not a valid codeQR={} rid={}", requestBodyJson.getCodeQR(), rid);
            return getErrorReason("Not a valid queue code.", MOBILE_JSON);
        } else if (!businessUserStoreService.hasAccess(rid, requestBodyJson.getCodeQR())) {
            LOG.info("Un-authorized store access to /api/m/mq/modify by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            StoreHourEntity storeHour = queueMobileService.updateQueueStateForToday(
                    requestBodyJson.getCodeQR(),
                    requestBodyJson.isDayClosed(),
                    requestBodyJson.isPreventJoining());

            return new JsonModifyQueue(requestBodyJson.getCodeQR(), storeHour).asJson();
        } catch (Exception e) {
            LOG.error("Failed getting queues reason={}", e.getLocalizedMessage(), e);
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        }
    }
}
