package com.noqapp.mobile.view.controller.open;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.json.JsonSurvey;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.service.SurveyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * User: hitender
 * Date: 10/22/19 1:44 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/survey")
public class SurveyResponseController {
    private static final Logger LOG = LoggerFactory.getLogger(SurveyResponseController.class);

    private SurveyService surveyService;
    private ApiHealthService apiHealthService;

    @Autowired
    public SurveyResponseController(
        SurveyService surveyService,
        ApiHealthService apiHealthService
    ) {
        this.surveyService = surveyService;
        this.apiHealthService = apiHealthService;
    }

    /** Get state of queue at the store. */
    @PostMapping(
        value = "/response",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String surveyResponse(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestBody
        JsonSurvey jsonSurvey
    ) {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Survey Response did={} dt={} bizNameId={} bizStoreId={}", did, deviceType, jsonSurvey.getBizNameId(), jsonSurvey.getBizStoreId());

        try {
            jsonSurvey.setDid(did.getText());
            surveyService.saveSurveyResponse(jsonSurvey);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed saving survey response bizStoreId={} did={} reason={}", jsonSurvey.getBizStoreId(), did, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/response",
                "surveyResponse",
                SurveyResponseController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
