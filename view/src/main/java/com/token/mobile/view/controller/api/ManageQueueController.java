package com.token.mobile.view.controller.api;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.token.domain.json.JsonToken;
import com.token.domain.types.QueueStateEnum;
import com.token.mobile.common.util.ErrorEncounteredJson;
import com.token.mobile.common.util.MobileSystemErrorCodeEnum;
import com.token.mobile.service.AuthenticateMobileService;
import com.token.mobile.service.QueueMobileService;
import com.token.service.BusinessUserStoreService;
import com.token.utils.ParseJsonStringToMap;
import com.token.utils.ScrubbedInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
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
@RequestMapping (value = "/api/mq")
public class ManageQueueController {
    private static final Logger LOG = LoggerFactory.getLogger(ManageQueueController.class);

    public static final String AUTH_KEY_HIDDEN = "*********";
    public static final String UNAUTHORIZED = "Unauthorized";

    private AuthenticateMobileService authenticateMobileService;
    private QueueMobileService queueMobileService;
    private BusinessUserStoreService businessUserStoreService;

    @Autowired
    public ManageQueueController(
            AuthenticateMobileService authenticateMobileService,
            QueueMobileService queueMobileService,
            BusinessUserStoreService businessUserStoreService
    ) {
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
    public List<JsonToken> getQueues(
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
            LOG.info("Un-authorized access to /api/mq by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            return businessUserStoreService.getQueues(rid);
        } catch (Exception e) {
            LOG.error("Getting queues reason={}", e.getLocalizedMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get state of queue at the store.
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
            value = "/a",
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
            LOG.info("Un-authorized access to /api/mq by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
            String codeQR = map.containsKey("c") ? map.get("c").getText() : null;

            if (StringUtils.isBlank(codeQR)) {
                LOG.warn("Not a valid codeQR={} rid={}", codeQR, rid);
                Map<String, String> errors = getErrorUserInput("Not a valid queue code.");
                return ErrorEncounteredJson.toJson(errors);
            } else if (!businessUserStoreService.hasAccess(rid, codeQR)) {
                LOG.info("Un-authorized store access to /api/mq by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            String serveNumberString = map.containsKey("s") ? map.get("s").getText() : null;
            int servedNumber;
            if (StringUtils.isNumeric(serveNumberString)) {
                servedNumber = Integer.valueOf(serveNumberString);
            } else {
                LOG.warn("Not a valid number={} codeQR={} rid={}", serveNumberString, codeQR, rid);
                Map<String, String> errors = getErrorUserInput("Not a valid number.");
                return ErrorEncounteredJson.toJson(errors);
            }

            QueueStateEnum queueState;
            try {
                queueState = map.containsKey("q") ? QueueStateEnum.valueOf(map.get("q").getText()) : null;
            } catch (IllegalArgumentException e) {
                LOG.error("Failed finding QueueState reason={}", e.getLocalizedMessage(), e);
                Map<String, String> errors = getErrorUserInput("Not a valid queue state.");
                return ErrorEncounteredJson.toJson(errors);
            }

            JsonToken jsonToken = queueMobileService.updateAndGetNextInQueue(codeQR, servedNumber, queueState);
            if (null == jsonToken) {
                LOG.error("Could not find queue codeQR={} servedNumber={} queueState={}", codeQR, servedNumber, queueState);
                Map<String, String> errors = getErrorSevere("Something went wrong. Engineers are looking into this.");
                return ErrorEncounteredJson.toJson(errors);
            }

            return jsonToken.asJson();
        } catch (JsonMappingException e) {
            LOG.error("Fail parsing json={} rid={} message={}", requestBodyJson, rid, e.getLocalizedMessage(), e);
            Map<String, String> errors = getErrorSevere("Something went wrong. Engineers are looking into this.");
            return ErrorEncounteredJson.toJson(errors);
        }
    }

    static Map<String, String> getErrorUserInput(String reason) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, reason);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MobileSystemErrorCodeEnum.USER_INPUT.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MobileSystemErrorCodeEnum.USER_INPUT.getCode());
        return errors;
    }

    static Map<String, String> getErrorSevere(String reason) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ErrorEncounteredJson.REASON, reason);
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MobileSystemErrorCodeEnum.SEVERE.name());
        errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MobileSystemErrorCodeEnum.SEVERE.getCode());
        return errors;
    }
}
