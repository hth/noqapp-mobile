package com.noqapp.mobile.view.controller.api.client;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.INCIDENT_EVENT_INCORRECT;
import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.domain.types.IncidentEventEnum.SOSP;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.CommonUtil;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.IncidentEventEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonIncidentEvent;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.search.elastic.helper.DomainConversion;
import com.noqapp.search.elastic.service.IncidentEventElasticService;
import com.noqapp.service.AccountService;
import com.noqapp.service.IncidentEvenService;

import org.apache.commons.lang3.StringUtils;

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
 * 5/21/21 10:00 AM
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/ie")
public class IncidentEventAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentEventAPIController.class);

    private IncidentEvenService incidentEvenService;
    private IncidentEventElasticService incidentEventElasticService;
    private AuthenticateMobileService authenticateMobileService;
    private AccountService accountService;
    private ApiHealthService apiHealthService;

    @Autowired
    public IncidentEventAPIController(
        IncidentEvenService incidentEvenService,
        IncidentEventElasticService incidentEventElasticService,
        AuthenticateMobileService authenticateMobileService,
        AccountService accountService,
        ApiHealthService apiHealthService
    ) {
        this.incidentEvenService = incidentEvenService;
        this.incidentEventElasticService = incidentEventElasticService;
        this.authenticateMobileService = authenticateMobileService;
        this.accountService = accountService;
        this.apiHealthService = apiHealthService;
    }

    /** Add incident to db and notify people in surrounding areas. */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String add(
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
        JsonIncidentEvent jsonIncidentEvent,

        HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("IncidentEvent Add for mail={} auth={} did={} dt={} appFlavor={}", mail, AUTH_KEY_HIDDEN, did, dt, appFlavor);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            if (StringUtils.isBlank(jsonIncidentEvent.getTitle())) {
                jsonIncidentEvent.setTitle(jsonIncidentEvent.getIncidentEvent().getDescription() + " reported in your vicinity");
            }
            IncidentEventEntity incidentEvent = populateFrom(jsonIncidentEvent, qid);
            incidentEvenService.save(incidentEvent);
            incidentEventElasticService.save(DomainConversion.getAsIncidentEventElastic(incidentEvent));
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing incidentEvent reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/",
                "add",
                IncidentEventAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "add",
                IncidentEventAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }

    /**
     * Add incident to db and notify people in surrounding areas.
     * Note: Merchant SOS is going to be different as it will be sent to merchant. //TODO implement
     */
    @PostMapping(
        value = "/sos",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String sos(
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
        JsonIncidentEvent jsonIncidentEvent,

        HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("IncidentEvent SOS for mail={} auth={} did={} dt={} appFlavor={}", mail, AUTH_KEY_HIDDEN, did, dt, appFlavor);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        try {
            if (SOSP != jsonIncidentEvent.getIncidentEvent()) {
                LOG.warn("Incorrect incident sent {} {} {}", jsonIncidentEvent.getIncidentEvent(), qid, did);
                return getErrorReason("Please supply with corrected detail or contact support", INCIDENT_EVENT_INCORRECT);
            }
            LOG.info("SOS activated by {} {} {}", qid, jsonIncidentEvent.getCountry(), jsonIncidentEvent.getCoordinate());

            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(qid);
            jsonIncidentEvent.setTitle(userProfile.getName() + " activated SOS");
            IncidentEventEntity incidentEvent = populateFrom(jsonIncidentEvent, qid);
            incidentEvenService.save(incidentEvent);
            incidentEventElasticService.save(DomainConversion.getAsIncidentEventElastic(incidentEvent));
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing incidentEvent reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/",
                "add",
                IncidentEventAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/",
                "add",
                IncidentEventAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }

    //TODO add when message is viewed

    private IncidentEventEntity populateFrom(JsonIncidentEvent jsonIncidentEvent, String qid) {
        IncidentEventEntity incidentEvent = new IncidentEventEntity()
            .setIncidentEvent(jsonIncidentEvent.getIncidentEvent())
            .setQid(qid)
            .setCoordinate(jsonIncidentEvent.getCoordinate())
            .setAddress(jsonIncidentEvent.getAddress())
            .setArea(jsonIncidentEvent.getArea())
            .setTown(jsonIncidentEvent.getTown())
            .setDistrict(jsonIncidentEvent.getDistrict())
            .setState(jsonIncidentEvent.getState())
            .setStateShortName(jsonIncidentEvent.getStateShortName())
            .setPostalCode(jsonIncidentEvent.getPostalCode())
            .setCountry(jsonIncidentEvent.getCountry())
            .setCountryShortName(jsonIncidentEvent.getCountryShortName())
            .setTitle(jsonIncidentEvent.getTitle())
            .setDescription(jsonIncidentEvent.getDescription());

        incidentEvent.setId(CommonUtil.generateHexFromObjectId());
        return incidentEvent;
    }
}
