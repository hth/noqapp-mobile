package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.body.client.ReviewRating;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.QueueMobileService;
import com.noqapp.mobile.service.TokenQueueMobileService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

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
    private ApiHealthService apiHealthService;

    @Autowired
    public ReviewAPIController(
            AuthenticateMobileService authenticateMobileService,
            TokenQueueMobileService tokenQueueMobileService,
            QueueMobileService queueMobileService,
            ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.queueMobileService = queueMobileService;
        this.apiHealthService = apiHealthService;
    }

    /**
     * Add review to service. This includes today's service or historical service.
     */
    @PostMapping(
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
            String requestBodyJson,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Review API for did={} dt={}", did, dt);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        ReviewRating reviewRating;
        try {
            reviewRating = new ObjectMapper().readValue(
                    requestBodyJson,
                    ReviewRating.class);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", requestBodyJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        boolean reviewSuccess = false;
        try {
            /* Required. */
            if (!tokenQueueMobileService.getBizService().isValidCodeQR(reviewRating.getCodeQR())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid token");
                return null;
            }

            reviewSuccess = queueMobileService.reviewService(
                    reviewRating.getCodeQR(),
                    reviewRating.getToken(),
                    did.getText(),
                    qid,
                    reviewRating.getRatingCount(),
                    reviewRating.getHoursSaved(),
                    reviewRating.getReview());
            return new JsonResponse(reviewSuccess).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing review reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/service",
                    "service",
                    ReviewAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return new JsonResponse(reviewSuccess).asJson();
        } finally {
            apiHealthService.insert(
                    "/service",
                    "service",
                    ReviewAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
