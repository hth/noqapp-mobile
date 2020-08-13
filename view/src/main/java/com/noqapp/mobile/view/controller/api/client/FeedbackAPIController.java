package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.body.client.Feedback;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.service.FeedbackService;

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
 * hitender
 * 10/4/18 4:57 PM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/feedback")
public class FeedbackAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackAPIController.class);

    private AuthenticateMobileService authenticateMobileService;
    private FeedbackService feedbackService;
    private ApiHealthService apiHealthService;

    @Autowired
    public FeedbackAPIController(
        AuthenticateMobileService authenticateMobileService,
        FeedbackService feedbackService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.feedbackService = feedbackService;
        this.apiHealthService = apiHealthService;
    }

    /** Add review to service. This includes today's service or historical service. */
    @PostMapping(
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String feedback(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt,

        @RequestHeader ("X-R-AF")
        ScrubbedInput appFlavor,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        Feedback feedbackJson,

        HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Feedback API for mail={} auth={} did={} dt={} appFlavor={}", mail, AUTH_KEY_HIDDEN, did, dt, appFlavor);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            feedbackService.submitFeedback(qid, feedbackJson);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing feedback reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/",
                "feedback",
                FeedbackAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "feedback",
                FeedbackAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }
}
