package com.noqapp.mobile.view.controller.api.merchant;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.BUSINESS_CUSTOMER_ID_EXISTS;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.USER_NOT_FOUND;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.common.utils.Validate;
import com.noqapp.domain.BusinessCustomerEntity;
import com.noqapp.domain.BusinessUserStoreEntity;
import com.noqapp.domain.UserProfileEntity;
import com.noqapp.domain.json.JsonBusinessCustomer;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.AccountService;
import com.noqapp.service.BusinessCustomerService;
import com.noqapp.service.BusinessUserStoreService;
import com.noqapp.service.QueueService;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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

    /** Add Business Customer Id to existing QID. */
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
            JsonBusinessCustomer json = new ObjectMapper().readValue(requestBodyJson, JsonBusinessCustomer.class);
            if (StringUtils.isBlank(json.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", json.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, json.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/bc/addId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            try {
                Assert.isTrue(Validate.isValidQid(json.getQueueUserId()), "Queue user id not valid");
            } catch (Exception e) {
                LOG.error("Failed as qid is null reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, json.getCodeQR());
            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByQid(json.getQueueUserId(), businessUserStore.getBizNameId());
            if (null != businessCustomer) {
                LOG.info("Found existing business customer qid={} codeQR={} businessQid={} bizNameId={} businessCustomerId={}",
                    qid, json.getCodeQR(), json.getQueueUserId(), businessUserStore.getBizNameId(), businessCustomer.getBusinessCustomerId());
                return getErrorReason("Business customer id already exists", BUSINESS_CUSTOMER_ID_EXISTS);
            }

            UserProfileEntity userProfile = accountService.findProfileByQueueUserId(json.getQueueUserId());
            if (null == userProfile) {
                /* Likely hood of reach here is zero, but if you do reach, then do investigate. */

                Map<String, String> errors = new HashMap<>();
                errors.put(ErrorEncounteredJson.REASON, "No user found. Would you like to register?");
                errors.put(AccountMobileService.ACCOUNT_REGISTRATION.PH.name(), json.getCustomerPhone());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, USER_NOT_FOUND.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, USER_NOT_FOUND.getCode());
                return ErrorEncounteredJson.toJson(errors);
            }

            businessCustomerService.addBusinessCustomer(
                    userProfile.getQueueUserId(),
                    json.getCodeQR(),
                    businessUserStore.getBizNameId(),
                    json.getBusinessCustomerId());
            LOG.info("Added business customer number to qid={} businessCustomerId={}", userProfile.getQueueUserId(), json.getBusinessCustomerId());
            return queueService.findThisPersonInQueue(userProfile.getQueueUserId(), json.getCodeQR());
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} reason={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } catch (Exception e) {
            LOG.error("Failed adding customer id qid={} reason={}", qid, e.getLocalizedMessage(), e);
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

    /** Edit Business Customer Id to existing QID. */
    @PostMapping(
            value = "/editId",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String editBusinessCustomerId(
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
            LOG.warn("Un-authorized access to /api/m/bc/editId by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            JsonBusinessCustomer json = new ObjectMapper().readValue(
                    requestBodyJson,
                    JsonBusinessCustomer.class);

            if (StringUtils.isBlank(json.getCodeQR())) {
                LOG.warn("Not a valid codeQR={} qid={}", json.getCodeQR(), qid);
                return getErrorReason("Not a valid queue code.", MOBILE_JSON);
            } else if (!businessUserStoreService.hasAccess(qid, json.getCodeQR())) {
                LOG.info("Un-authorized store access to /api/m/bc/editId by mail={}", mail);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
                return null;
            }

            try {
                Assert.isTrue(Validate.isValidQid(json.getQueueUserId()), "Queue user id not valid");
            } catch (Exception e) {
                LOG.error("Failed as qid is null reason={}", e.getLocalizedMessage(), e);
                return getErrorReason("Not a valid queue status.", MOBILE_JSON);
            }

            BusinessUserStoreEntity businessUserStore = businessUserStoreService.findOneByQidAndCodeQR(qid, json.getCodeQR());
            BusinessCustomerEntity businessCustomer = businessCustomerService.findOneByQid(
                    json.getQueueUserId(),
                    businessUserStore.getBizNameId());
            if (null == businessCustomer) {
                LOG.warn("Could not find customer with qid={} bizNameId={}",json.getQueueUserId(), businessUserStore.getBizNameId());
                return getErrorReason("Business customer id does not exists", BUSINESS_CUSTOMER_ID_DOES_NOT_EXISTS);
            }

            businessCustomer = businessCustomerService.findOneByCustomerId(json.getBusinessCustomerId(), businessUserStore.getBizNameId());
            if (null != businessCustomer) {
                LOG.warn("Found existing business customer qid={} codeQR={} businessQid={} bizNameId={} businessCustomerId={}",
                    qid, json.getCodeQR(), json.getQueueUserId(), businessUserStore.getBizNameId(), businessCustomer.getBusinessCustomerId());
                return getErrorReason("Business customer id already exists", BUSINESS_CUSTOMER_ID_EXISTS);
            }

            businessCustomerService.editBusinessCustomer(
                    json.getQueueUserId(),
                    json.getCodeQR(),
                    businessUserStore.getBizNameId(),
                    json.getBusinessCustomerId());
            LOG.info("Edit business customer number to qid={} businessCustomerId={}",
                    json.getQueueUserId(), json.getBusinessCustomerId());

            return queueService.findThisPersonInQueue(json.getQueueUserId(), json.getCodeQR());
        } catch (JsonMappingException e) {
            LOG.error("Failed parsing json={} qid={} reason={}", requestBodyJson, qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                    "/editId",
                    "editId",
                    BusinessCustomerController.class.getName(),
                    Duration.between(start, Instant.now()),
                    methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
