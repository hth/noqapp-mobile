package com.noqapp.mobile.view.controller.api.merchant;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BusinessCustomerEntity;
import com.noqapp.domain.QueueEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonBusinessCustomerLookup;
import com.noqapp.domain.json.JsonQueuePersonList;
import com.noqapp.domain.json.JsonQueuedPerson;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.domain.JsonProfile;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.QueueService;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

/**
 * Helps find client details when adding client to queue. Add client to business and find dependents.
 * hitender
 * 6/17/18 1:54 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/bc")
public class BusinessCustomerController {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessCustomerController.class);

    private AuthenticateMobileService authenticateMobileService;
    private AccountService accountService;
    private BusinessCustomerService businessCustomerService;
    private BusinessUserStoreService businessUserStoreService;
    private QueueService queueService;
    private ApiHealthService apiHealthService;

    @Autowired
    public BusinessCustomerController(
            AuthenticateMobileService authenticateMobileService,
            AccountService accountService,
            BusinessCustomerService businessCustomerService,
            BusinessUserStoreService businessUserStoreService,
            QueueService queueService,
            ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.accountService = accountService;
        this.businessCustomerService = businessCustomerService;
        this.businessUserStoreService = businessUserStoreService;
        this.queueService = queueService;
        this.apiHealthService = apiHealthService;
    }

    @PostMapping(
            value = "/addId",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String addBusinessCustomerId(
            @RequestHeader("X-R-DID")
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
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/bc/addId by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomerLookup businessCustomerLookup = new ObjectMapper().readValue(
                    requestBodyJson,
                    JsonBusinessCustomerLookup.class);

            if (StringUtils.isBlank(businessCustomerLookup.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomerLookup.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomerLookup.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/bc/addId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByCustomerId(
                    businessCustomerLookup.getBusinessCustomerId(),
                    businessCustomerLookup.getCodeQR());

            UserProfileEntity userProfile = null;
            if (businessCustomer == null) {
                userProfile = accountService.checkUserExistsByPhone(businessCustomerLookup.getCustomerPhone());
            }

            if (userProfile == null) {
                /* Likely hood of reach here is zero, but if you do reach, then do investigate. */

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), businessCustomerLookup.getCustomerPhone());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            businessCustomerService.addBusinessCustomer(userProfile.getQueueUserId(), businessCustomerLookup.getCodeQR(), businessCustomerLookup.getBusinessCustomerId());
            LOG.info("Added business customer number to qid={} businessCustomerId={}", userProfile.getQueueUserId(), businessCustomerLookup.getBusinessCustomerId());

            List<QueueEntity> queues = queueService.findAllQueuedByQid(userProfile.getQueueUserId());
            List<JsonQueuedPerson> queuedPeople = new ArrayList<>();
            queueService.populateInJsonQueuePersonList(queuedPeople, queues);
            return new JsonQueuePersonList().setQueuedPeople(queuedPeople).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/addId",
                    "addId",
                    BusinessCustomerController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
            value = "/byId",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String findByBusinessCustomerId(
            @RequestHeader("X-R-DID")
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
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/bc/byId by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomerLookup businessCustomerLookup = new ObjectMapper().readValue(
                    requestBodyJson,
                    JsonBusinessCustomerLookup.class);

            if (StringUtils.isBlank(businessCustomerLookup.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomerLookup.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomerLookup.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/bc/byId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            UserProfileEntity userProfile = businessCustomerService.findByBusinessCustomerId(businessCustomerLookup.getBusinessCustomerId(), businessCustomerLookup.getCodeQR());
            return JsonProfile.newInstance(userProfile, 0).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/byId",
                    "byId",
                    BusinessCustomerController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
            value = "/byPhone",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String findByPhone(
            @RequestHeader("X-R-DID")
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
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("All queues associated with mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/bc/byPhone by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomerLookup businessCustomerLookup = new ObjectMapper().readValue(
                    requestBodyJson,
                    JsonBusinessCustomerLookup.class);

            if (StringUtils.isBlank(businessCustomerLookup.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", businessCustomerLookup.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, businessCustomerLookup.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/bc/byPhone by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            UserProfileEntity userProfile = accountService.checkUserExistsByPhone(businessCustomerLookup.getCustomerPhone());
            return JsonProfile.newInstance(userProfile, 0).asJson();
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} message={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/byPhone",
                    "byPhone",
                    BusinessCustomerController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
