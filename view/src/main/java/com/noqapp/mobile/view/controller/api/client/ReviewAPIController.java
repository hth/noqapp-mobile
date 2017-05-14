package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

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
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.utils.ParseJsonStringToMap;
import com.noqapp.utils.ScrubbedInput;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 4/28/17 11:15 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping (value = "/api/c/review")
public class ReviewAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewAPIController.class);

    private AuthenticateMobileService authenticateMobileService;
    private TokenQueueMobileService tokenQueueMobileService;
    private QueueMobileService queueMobileService;

    @Autowired
    public ReviewAPIController(
            AuthenticateMobileService authenticateMobileService,
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
    }

    /**
     * Add review to service.
     *
     * @param did
     * @param dt
     * @param mail
     * @param auth
     * @param bodyJson
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/service",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String service(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String bodyJson,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("On scan get state did={} dt={}", did, dt);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(bodyJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", bodyJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        boolean reviewSuccess = false;
        try {
            /* Required. */
            String codeQR = map.get("codeQR").getText();
            int token = Integer.parseInt(map.get("t").getText());
            int ratingCount = Integer.parseInt(map.get("ra").getText());
            int hoursSaved = Integer.parseInt(map.get("hr").getText());

            if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            reviewSuccess = queueMobileService.reviewService(codeQR, token, did.getText(), rid, ratingCount, hoursSaved);
            return new JsonResponse(reviewSuccess).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review reason={}", e.getLocalizedMessage(), e);
            return new JsonResponse(reviewSuccess).asJson();
        }
    }

    /**
     * Add review to service.
     *
     * @param did
     * @param dt
     * @param mail
     * @param auth
     * @param bodyJson
     * @param response
     * @return
     * @throws IOException
     */
    @Timed
    @ExceptionMetered
    @RequestMapping (
            method = RequestMethod.POST,
            value = "/historical/service",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String serviceHistorical(
            @RequestHeader ("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String bodyJson,

            HttpServletResponse response
    ) throws IOException {
        LOG.info("On scan get state did={} dt={}", did, dt);
        String rid = authenticateMobileService.getReceiptUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, rid)) return null;

        Map<String, ScrubbedInput> map;
        try {
            map = ParseJsonStringToMap.jsonStringToMap(bodyJson);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", bodyJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        boolean reviewSuccess = false;
        try {
            /* Required. */
            String codeQR = map.get("codeQR").getText();
            int ratingCount = Integer.parseInt(map.get("ra").getText());
            int hoursSaved = Integer.parseInt(map.get("hr").getText());

            if (!tokenQueueMobileService.getBizService().isValidCodeQR(codeQR)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            reviewSuccess = queueMobileService.reviewHistoricalService(codeQR, did.getText(), rid, ratingCount, hoursSaved);
            return new JsonResponse(reviewSuccess).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review for historical reason={}", e.getLocalizedMessage(), e);
            return new JsonResponse(reviewSuccess).asJson();
        }
    }
}
