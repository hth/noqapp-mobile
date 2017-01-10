package com.token.mobile.view.controller.api;

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
import com.token.mobile.service.AuthenticateMobileService;
import com.token.mobile.service.QueueMobileService;
import com.token.utils.ParseJsonStringToMap;
import com.token.utils.ScrubbedInput;

import java.io.IOException;
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

    @Autowired
    public ManageQueueController(AuthenticateMobileService authenticateMobileService, QueueMobileService queueMobileService) {
        this.authenticateMobileService = authenticateMobileService;
        this.queueMobileService = queueMobileService;
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
            value = "/served",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public JsonToken getState(
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
        LOG.info("On scanned code get state did={} dt={} mail={} auth={}", did, dt, AUTH_KEY_HIDDEN);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (null == rid) {
            LOG.info("Un-authorized access to /api/mq by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        Map<String, ScrubbedInput> map = ParseJsonStringToMap.jsonStringToMap(requestBodyJson);
        String codeQR = map.containsKey("c") ? map.get("c").getText() : null;
        int servedNumber = map.containsKey("s") ? Integer.valueOf(map.get("s").getText()) : null;
        QueueStateEnum queueState = map.containsKey("q") ? QueueStateEnum.valueOf(map.get("q").getText()) : null;

        JsonToken jsonToken = queueMobileService.updateAndGetNextInQueue(codeQR, servedNumber, queueState);
        if (null == jsonToken) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
            return null;
        }

        return jsonToken;
    }
}
